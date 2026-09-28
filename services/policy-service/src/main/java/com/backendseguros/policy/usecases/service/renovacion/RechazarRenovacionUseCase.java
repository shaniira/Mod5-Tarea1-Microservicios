package com.backendseguros.policy.usecases.service.renovacion;

import static com.backendseguros.policy.usecases.mapper.RenovacionResponseMapper.toResponse;

import com.backendseguros.policy.usecases.dto.Responses.RenovacionResponse;
import com.backendseguros.policy.entities.exception.RecursoNoEncontradoException;
import com.backendseguros.policy.entities.model.PropuestaRenovacion;
import com.backendseguros.policy.usecases.port.out.repository.RenovacionRepository;
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
