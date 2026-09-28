package com.backendseguros.claims.usecases.service.siniestro;

import static com.backendseguros.claims.usecases.mapper.SiniestroResponseMapper.toResponse;

import com.backendseguros.claims.entities.exception.RecursoNoEncontradoException;
import com.backendseguros.claims.entities.exception.ReglaNegocioException;
import com.backendseguros.claims.entities.model.PolizaRef;
import com.backendseguros.claims.entities.model.Siniestro;
import com.backendseguros.claims.entities.valueobject.Dinero;
import com.backendseguros.claims.usecases.dto.RegistrarSiniestroRequestModel;
import com.backendseguros.claims.usecases.dto.Responses.SiniestroResponse;
import com.backendseguros.claims.usecases.mapper.SiniestroEventMapper;
import com.backendseguros.claims.usecases.port.out.event.DomainEventPublisherPort;
import com.backendseguros.claims.usecases.port.out.id.IdGeneratorPort;
import com.backendseguros.claims.usecases.port.out.repository.PolizaRefRepository;
import com.backendseguros.claims.usecases.port.out.repository.SiniestroRepository;
import com.backendseguros.claims.usecases.port.out.time.ClockPort;
import com.backendseguros.claims.usecases.port.out.transaccion.TransaccionPort;

/**
 * Registra un siniestro validando la póliza contra la proyección policy_ref (paso 4.4), no contra
 * la base de pólizas. Si la proyección todavía no tiene la póliza se responde "no encontrada", sin
 * consultar a nadie más. El siniestro y su claim.registered.v1 se guardan en una sola transacción.
 */
public class RegistrarSiniestroUseCase {

    private final PolizaRefRepository polizas;
    private final SiniestroRepository siniestros;
    private final DomainEventPublisherPort eventos;
    private final TransaccionPort transaccion;
    private final ClockPort clock;
    private final IdGeneratorPort ids;

    public RegistrarSiniestroUseCase(
            PolizaRefRepository polizas,
            SiniestroRepository siniestros,
            DomainEventPublisherPort eventos,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        this.polizas = polizas;
        this.siniestros = siniestros;
        this.eventos = eventos;
        this.transaccion = transaccion;
        this.clock = clock;
        this.ids = ids;
    }

    public SiniestroResponse execute(RegistrarSiniestroRequestModel solicitud) {
        PolizaRef poliza =
                polizas.buscarPorId(solicitud.polizaId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Póliza"));

        if (!poliza.estaVigente()) {
            throw new ReglaNegocioException(
                    "POLIZA_NO_VIGENTE", "Solo se registran siniestros en pólizas vigentes");
        }

        Siniestro siniestro =
                new Siniestro(
                        ids.generar(),
                        poliza.polizaId(),
                        solicitud.fecha(),
                        solicitud.tipo(),
                        Dinero.soles(solicitud.montoEstimado()),
                        solicitud.responsabilidadAsegurado(),
                        solicitud.gravedad(),
                        solicitud.estado());

        Siniestro guardado =
                transaccion.ejecutar(
                        () -> {
                            Siniestro resultado = siniestros.guardar(siniestro);
                            eventos.publicar(
                                    SiniestroEventMapper.registrado(
                                            ids.generar(), clock.now(), resultado, poliza.clienteId()));
                            return resultado;
                        });

        return toResponse(guardado);
    }
}
