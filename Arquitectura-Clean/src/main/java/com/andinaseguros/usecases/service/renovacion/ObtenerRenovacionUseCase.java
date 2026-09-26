package com.andinaseguros.usecases.service.renovacion;

import static com.andinaseguros.usecases.mapper.RenovacionResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.RenovacionResponse;
import com.andinaseguros.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.entities.model.PropuestaRenovacion;
import com.andinaseguros.usecases.port.out.repository.RenovacionRepository;
import java.util.UUID;

public class ObtenerRenovacionUseCase {

    private final RenovacionRepository renovacionRepository;

    public ObtenerRenovacionUseCase(RenovacionRepository renovacionRepository) {
        this.renovacionRepository = renovacionRepository;
    }

    public RenovacionResponse execute(UUID renovacionId) {
        PropuestaRenovacion propuesta =
                renovacionRepository
                        .buscarPorId(renovacionId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Renovación"));

        return toResponse(propuesta);
    }
}
