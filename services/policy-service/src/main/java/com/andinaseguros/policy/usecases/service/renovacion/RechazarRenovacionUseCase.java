package com.andinaseguros.policy.usecases.service.renovacion;

import static com.andinaseguros.policy.usecases.mapper.RenovacionResponseMapper.toResponse;

import com.andinaseguros.policy.usecases.dto.Responses.RenovacionResponse;
import com.andinaseguros.policy.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.policy.entities.model.PropuestaRenovacion;
import com.andinaseguros.policy.usecases.port.out.repository.RenovacionRepository;
import java.time.LocalDateTime;
import java.util.UUID;

public class RechazarRenovacionUseCase {

    private final RenovacionRepository renovacionRepository;

    public RechazarRenovacionUseCase(RenovacionRepository renovacionRepository) {
        this.renovacionRepository = renovacionRepository;
    }

    public RenovacionResponse execute(UUID renovacionId) {
        PropuestaRenovacion propuesta =
                renovacionRepository
                        .buscarPorId(renovacionId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Renovación"));

        propuesta.rechazar(LocalDateTime.now());

        PropuestaRenovacion propuestaGuardada = renovacionRepository.guardar(propuesta);

        return toResponse(propuestaGuardada);
    }
}
