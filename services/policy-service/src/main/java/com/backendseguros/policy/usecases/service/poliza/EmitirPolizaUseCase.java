package com.backendseguros.policy.usecases.service.poliza;

import static com.backendseguros.policy.usecases.mapper.PolizaResponseMapper.toResponse;

import com.backendseguros.policy.entities.enums.EstadoPoliza;
import com.backendseguros.policy.entities.event.EmisionRechazadaEvent;
import com.backendseguros.policy.entities.exception.ReglaNegocioException;
import com.backendseguros.policy.entities.model.CotizacionAceptada;
import com.backendseguros.policy.entities.model.Poliza;
import com.backendseguros.policy.entities.valueobject.PeriodoVigencia;
import com.backendseguros.policy.usecases.dto.EmitirPolizaRequestModel;
import com.backendseguros.policy.usecases.dto.Responses.PolizaResponse;
import com.backendseguros.policy.usecases.exception.CotizacionYaEmitidaException;
import com.backendseguros.policy.usecases.mapper.PolizaEventMapper;
import com.backendseguros.policy.usecases.port.out.event.DomainEventPublisherPort;
import com.backendseguros.policy.usecases.port.out.id.IdGeneratorPort;
import com.backendseguros.policy.usecases.port.out.repository.CotizacionesAceptadasRepository;
import com.backendseguros.policy.usecases.port.out.repository.PolizaRepository;
import com.backendseguros.policy.usecases.port.out.time.ClockPort;
import com.backendseguros.policy.usecases.port.out.transaccion.TransaccionPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Saga de emisión de póliza (coreografía, pasos 6.3 y 6.4):
 *
 * <ol>
 *   <li>La cotización debe estar en accepted_quotes (llegó por quote.accepted.v1): no se consulta a
 *       quotation.
 *   <li>Si ya tiene póliza o ya venció, se rechaza y se publica la compensación
 *       policy.issuance-rejected.v1; quotation registra el motivo.
 *   <li>Si es válida, se crea la póliza VIGENTE y se publica policy.issued.v1 en la misma
 *       transacción (Outbox); quotation la marca EMITIDA, claims la agrega a policy_ref y
 *       notification envía el WhatsApp.
 * </ol>
 *
 * El índice único por cotizacionId impide la doble emisión aunque dos solicitudes lleguen a la vez.
 * Diferencia con el monolito: el monolito no rechazaba una cotización aceptada ya vencida; la ruta
 * pide rechazarla (paso 6.4).
 */
public class EmitirPolizaUseCase {
    public static final String VENCIDA = "COTIZACION_VENCIDA";
    // 5 intentos con espera creciente y aleatoria (~1,5 s en total): alcanza para que la
    // transacción que ganó termine, aun con el servicio recién arrancado.
    static final int MAX_INTENTOS = 5;
    static final long ESPERA_ENTRE_INTENTOS_MS = 100;

    private final CotizacionesAceptadasRepository cotizaciones;
    private final PolizaRepository polizas;
    private final DomainEventPublisherPort eventos;
    private final TransaccionPort transaccion;
    private final ClockPort clock;
    private final IdGeneratorPort ids;

    public EmitirPolizaUseCase(
            CotizacionesAceptadasRepository cotizaciones,
            PolizaRepository polizas,
            DomainEventPublisherPort eventos,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        this.cotizaciones = cotizaciones;
        this.polizas = polizas;
        this.eventos = eventos;
        this.transaccion = transaccion;
        this.clock = clock;
        this.ids = ids;
    }

    public PolizaResponse execute(EmitirPolizaRequestModel solicitud) {
        CotizacionAceptada cotizacion =
                cotizaciones
                        .buscar(solicitud.cotizacionId())
                        .orElseThrow(
                                () ->
                                        new ReglaNegocioException(
                                                "COTIZACION_NO_ACEPTADA",
                                                "La cotización debe estar aceptada antes de emitir la póliza"));

        // Dos emisiones simultáneas de la misma cotización chocan en el índice único; dentro de una
        // transacción MongoDB lo informa como un conflicto transitorio (WriteConflict) y pide
        // reintentar. En el reintento la validación ya ve la póliza de la otra solicitud.
        for (int intento = 1; ; intento++) {
            try {
                return intentarEmitir(cotizacion, solicitud.inicioVigencia());
            } catch (ReglaNegocioException reglaDeNegocio) {
                throw reglaDeNegocio;
            } catch (RuntimeException conflicto) {
                if (intento == MAX_INTENTOS) {
                    throw conflicto;
                }
                esperar(intento);
            }
        }
    }

    private PolizaResponse intentarEmitir(CotizacionAceptada cotizacion, LocalDate inicio) {
        var existente = polizas.buscarPorCotizacionId(cotizacion.cotizacionId());
        if (existente.isPresent()) {
            throw rechazar(cotizacion.cotizacionId(), new CotizacionYaEmitidaException(), existente.get().getId());
        }
        if (cotizacion.vencidaEn(LocalDateTime.ofInstant(clock.now(), ZoneOffset.UTC))) {
            throw rechazar(
                    cotizacion.cotizacionId(),
                    new ReglaNegocioException(VENCIDA, "La cotización venció antes de emitir la póliza"),
                    null);
        }

        Poliza poliza =
                new Poliza(
                        ids.generar(),
                        "POL-"
                                + Year.from(clock.now().atZone(ZoneOffset.UTC)).getValue()
                                + "-"
                                + ids.generar().toString().substring(0, 8).toUpperCase(),
                        cotizacion.cotizacionId(),
                        cotizacion.clienteId(),
                        cotizacion.vehiculoId(),
                        cotizacion.prima(),
                        new PeriodoVigencia(inicio, inicio.plusYears(1)),
                        EstadoPoliza.VIGENTE,
                        null,
                        1);
        try {
            Poliza guardada =
                    transaccion.ejecutar(
                            () -> {
                                Poliza resultado = polizas.guardar(poliza);
                                eventos.publicar(PolizaEventMapper.emitida(ids.generar(), clock.now(), resultado));
                                return resultado;
                            });
            return toResponse(guardada);
        } catch (CotizacionYaEmitidaException carrera) {
            // Otra solicitud emitió la misma cotización entre la validación y el guardado.
            throw rechazar(cotizacion.cotizacionId(), carrera, null);
        }
    }

    private static void esperar(int intento) {
        try {
            // Espera aleatoria para que dos reintentos no vuelvan a chocar al mismo tiempo.
            Thread.sleep(ESPERA_ENTRE_INTENTOS_MS * intento
                    + java.util.concurrent.ThreadLocalRandom.current().nextLong(ESPERA_ENTRE_INTENTOS_MS));
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
        }
    }

    /** Compensación: publica policy.issuance-rejected.v1 y devuelve el error para el usuario. */
    private ReglaNegocioException rechazar(UUID cotizacionId, ReglaNegocioException motivo, UUID polizaExistenteId) {
        eventos.publicar(
                new EmisionRechazadaEvent(
                        ids.generar(), clock.now(), cotizacionId, motivo.getCodigo(), motivo.getMessage(), polizaExistenteId));
        return motivo;
    }
}
