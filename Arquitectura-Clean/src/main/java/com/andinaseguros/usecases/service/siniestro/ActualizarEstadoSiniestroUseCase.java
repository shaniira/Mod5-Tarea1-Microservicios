package com.andinaseguros.usecases.service.siniestro;

import static com.andinaseguros.usecases.mapper.SiniestroResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.SiniestroResponse;
import com.andinaseguros.entities.enums.EstadoSiniestro;
import com.andinaseguros.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.model.Siniestro;
import com.andinaseguros.usecases.port.out.repository.PolizaRepository;
import com.andinaseguros.usecases.port.out.repository.SiniestroRepository;
import java.util.UUID;

public class ActualizarEstadoSiniestroUseCase {

    private final PolizaRepository polizaRepository;
    private final SiniestroRepository siniestroRepository;

    public ActualizarEstadoSiniestroUseCase(
            PolizaRepository polizaRepository, SiniestroRepository siniestroRepository) {
        this.polizaRepository = polizaRepository;
        this.siniestroRepository = siniestroRepository;
    }

    public SiniestroResponse execute(UUID polizaId, UUID siniestroId, EstadoSiniestro nuevoEstado) {
        polizaRepository
                .buscarPorId(polizaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Póliza"));

        Siniestro siniestro =
                siniestroRepository
                        .buscarPorId(siniestroId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Siniestro"));

        validarPertenencia(siniestro, polizaId);

        siniestro.cambiarEstado(nuevoEstado);

        Siniestro siniestroGuardado = siniestroRepository.guardar(siniestro);

        return toResponse(siniestroGuardado);
    }

    private void validarPertenencia(Siniestro siniestro, UUID polizaId) {
        if (!siniestro.polizaId().equals(polizaId)) {
            throw new ReglaNegocioException(
                    "SINIESTRO_NO_PERTENECE", "El siniestro no pertenece a la póliza indicada");
        }
    }
}
