package com.andinaseguros.usecases.service.renovacion;

import static com.andinaseguros.usecases.mapper.RenovacionResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.RenovacionResponse;
import com.andinaseguros.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.entities.model.PropuestaRenovacion;
import com.andinaseguros.usecases.port.out.repository.RenovacionRepository;
import java.time.LocalDateTime;
import java.util.UUID;

public class AprobarRenovacionUseCase {

    private final RenovacionRepository renovacionRepository;

    public AprobarRenovacionUseCase(RenovacionRepository renovacionRepository) {
        this.renovacionRepository = renovacionRepository;
    }

    public RenovacionResponse execute(UUID renovacionId) {
        PropuestaRenovacion propuesta =
                renovacionRepository
                        .buscarPorId(renovacionId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Renovación"));

        propuesta.aprobar(LocalDateTime.now());

        PropuestaRenovacion propuestaGuardada = renovacionRepository.guardar(propuesta);

        return toResponse(propuestaGuardada);
    }
}
