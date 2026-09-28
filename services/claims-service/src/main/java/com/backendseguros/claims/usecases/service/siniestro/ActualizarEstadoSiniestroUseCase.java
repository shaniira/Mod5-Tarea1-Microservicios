package com.backendseguros.claims.usecases.service.siniestro;

import static com.backendseguros.claims.usecases.mapper.SiniestroResponseMapper.toResponse;

import com.backendseguros.claims.entities.enums.EstadoSiniestro;
import com.backendseguros.claims.entities.exception.RecursoNoEncontradoException;
import com.backendseguros.claims.entities.exception.ReglaNegocioException;
import com.backendseguros.claims.entities.model.Siniestro;
import com.backendseguros.claims.usecases.dto.Responses.SiniestroResponse;
import com.backendseguros.claims.usecases.mapper.SiniestroEventMapper;
import com.backendseguros.claims.usecases.port.out.event.DomainEventPublisherPort;
import com.backendseguros.claims.usecases.port.out.id.IdGeneratorPort;
import com.backendseguros.claims.usecases.port.out.repository.PolizaRefRepository;
import com.backendseguros.claims.usecases.port.out.repository.SiniestroRepository;
import com.backendseguros.claims.usecases.port.out.time.ClockPort;
import com.backendseguros.claims.usecases.port.out.transaccion.TransaccionPort;
import java.util.UUID;

/**
 * Cambia el estado de un siniestro y publica claim.status-changed.v1 en la misma transacción.
 * Mismas validaciones que el monolito (la póliza existe, el siniestro le pertenece y no está
 * cerrado); la póliza se busca en policy_ref.
 */
public class ActualizarEstadoSiniestroUseCase {

    private final PolizaRefRepository polizas;
    private final SiniestroRepository siniestros;
    private final DomainEventPublisherPort eventos;
    private final TransaccionPort transaccion;
    private final ClockPort clock;
    private final IdGeneratorPort ids;

    public ActualizarEstadoSiniestroUseCase(
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

    public SiniestroResponse execute(UUID polizaId, UUID siniestroId, EstadoSiniestro nuevoEstado) {
        polizas.buscarPorId(polizaId).orElseThrow(() -> new RecursoNoEncontradoException("Póliza"));

        Siniestro siniestro =
                siniestros
                        .buscarPorId(siniestroId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Siniestro"));

        if (!siniestro.polizaId().equals(polizaId)) {
            throw new ReglaNegocioException(
                    "SINIESTRO_NO_PERTENECE", "El siniestro no pertenece a la póliza indicada");
        }

        EstadoSiniestro anterior = siniestro.estado();
        siniestro.cambiarEstado(nuevoEstado);

        Siniestro guardado =
                transaccion.ejecutar(
                        () -> {
                            Siniestro resultado = siniestros.guardar(siniestro);
                            eventos.publicar(
                                    SiniestroEventMapper.estadoCambiado(
                                            ids.generar(), clock.now(), resultado, anterior));
                            return resultado;
                        });

        return toResponse(guardado);
    }
}
