package com.backendseguros.policy.usecases.service.renovacion;

import static com.backendseguros.policy.usecases.mapper.PolizaResponseMapper.toResponse;

import com.backendseguros.policy.entities.enums.EstadoPoliza;
import com.backendseguros.policy.entities.enums.EstadoRenovacion;
import com.backendseguros.policy.entities.exception.RecursoNoEncontradoException;
import com.backendseguros.policy.entities.exception.ReglaNegocioException;
import com.backendseguros.policy.entities.model.Poliza;
import com.backendseguros.policy.entities.model.PropuestaRenovacion;
import com.backendseguros.policy.entities.model.SiniestroRef;
import com.backendseguros.policy.entities.valueobject.PeriodoVigencia;
import com.backendseguros.policy.usecases.dto.Responses.PolizaResponse;
import com.backendseguros.policy.usecases.mapper.PolizaEventMapper;
import com.backendseguros.policy.usecases.port.out.event.DomainEventPublisherPort;
import com.backendseguros.policy.usecases.port.out.id.IdGeneratorPort;
import com.backendseguros.policy.usecases.port.out.repository.PolizaRepository;
import com.backendseguros.policy.usecases.port.out.repository.RenovacionRepository;
import com.backendseguros.policy.usecases.port.out.siniestros.HistorialSiniestrosPort;
import com.backendseguros.policy.usecases.port.out.time.ClockPort;
import com.backendseguros.policy.usecases.port.out.transaccion.TransaccionPort;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;

/**
 * Mismas reglas que el monolito (propuesta aprobada, una sola póliza por propuesta, vigencia desde
 * el día siguiente al fin de la anterior). Además, la póliza nueva, la anterior (RENOVADA), la
 * propuesta y policy.renewed.v1 se guardan en una sola transacción (paso 6.6).
 *
 * <p>Fase 7: antes de generar, los siniestros se confirman con claims-service (la fuente), no
 * con la copia claim_ref, que puede tener un evento de atraso. Es el paso irreversible, así que
 * aquí se elige consistencia sobre disponibilidad (CP): con un siniestro abierto, o si el
 * historial cambió desde la evaluación, no se renueva; si claims-service no responde, 503 y
 * reintentar. La evaluación sigue usando la copia local (rápida, AP).
 */
public class GenerarPolizaRenovadaUseCase {
    private final RenovacionRepository renovacionRepository;
    private final PolizaRepository polizaRepository;
    private final DomainEventPublisherPort eventos;
    private final TransaccionPort transaccion;
    private final ClockPort clock;
    private final IdGeneratorPort ids;
    private final HistorialSiniestrosPort historialSiniestros;

    public GenerarPolizaRenovadaUseCase(
            RenovacionRepository renovacionRepository,
            PolizaRepository polizaRepository,
            DomainEventPublisherPort eventos,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids,
            HistorialSiniestrosPort historialSiniestros) {
        this.renovacionRepository = renovacionRepository;
        this.polizaRepository = polizaRepository;
        this.eventos = eventos;
        this.transaccion = transaccion;
        this.clock = clock;
        this.ids = ids;
        this.historialSiniestros = historialSiniestros;
    }

    public PolizaResponse execute(UUID renovacionId) {
        PropuestaRenovacion propuesta =
                renovacionRepository
                        .buscarPorId(renovacionId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Renovación"));
        if (propuesta.estado() != EstadoRenovacion.ACEPTADA) {
            throw new ReglaNegocioException(
                    "RENOVACION_NO_APROBADA", "Debe aprobar la propuesta antes de generar la póliza");
        }
        if (propuesta.polizaRenovadaId() != null) {
            return toResponse(
                    polizaRepository
                            .buscarPorId(propuesta.polizaRenovadaId())
                            .orElseThrow(() -> new RecursoNoEncontradoException("Póliza")));
        }
        Poliza origen =
                polizaRepository
                        .buscarPorId(propuesta.polizaOrigenId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Póliza de origen"));

        confirmarSiniestros(propuesta);

        LocalDate inicio = origen.getVigencia().fin().plusDays(1);
        Poliza renovada =
                new Poliza(
                        ids.generar(),
                        "POL-REN-" + Year.from(inicio).getValue() + "-"
                                + ids.generar().toString().substring(0, 8).toUpperCase(),
                        null,
                        origen.getClienteId(),
                        origen.getVehiculoId(),
                        propuesta.nuevaPrima(),
                        new PeriodoVigencia(inicio, inicio.plusYears(1)),
                        EstadoPoliza.VIGENTE,
                        propuesta.id(),
                        1);
        origen.marcarRenovada();
        propuesta.vincularPolizaRenovada(renovada.getId());

        Poliza guardada =
                transaccion.ejecutar(
                        () -> {
                            Poliza nueva = polizaRepository.guardar(renovada);
                            Poliza anterior = polizaRepository.guardar(origen);
                            renovacionRepository.guardar(propuesta);
                            eventos.publicar(
                                    PolizaEventMapper.renovada(ids.generar(), clock.now(), nueva, anterior, propuesta.id()));
                            return nueva;
                        });
        return toResponse(guardada);
    }

    /** Confirma con claims-service que nada cambió desde la evaluación (lanza si no se puede renovar). */
    private void confirmarSiniestros(PropuestaRenovacion propuesta) {
        List<SiniestroRef> actuales = historialSiniestros.listarPorPoliza(propuesta.polizaOrigenId());
        if (actuales.stream().anyMatch(SiniestroRef::abierto)) {
            throw new ReglaNegocioException(
                    "SINIESTROS_PENDIENTES",
                    "La póliza tiene siniestros abiertos (confirmado con el servicio de siniestros). No se puede renovar.");
        }
        if (actuales.size() != propuesta.siniestrosConsiderados()) {
            throw new ReglaNegocioException(
                    "RENOVACION_DESACTUALIZADA",
                    "El historial de siniestros cambió desde la evaluación. Evalúa la renovación de nuevo.");
        }
    }
}
