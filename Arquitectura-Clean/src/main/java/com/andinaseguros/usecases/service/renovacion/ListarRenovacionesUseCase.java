package com.andinaseguros.usecases.service.renovacion;

import static com.andinaseguros.usecases.mapper.RenovacionResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.RenovacionResponse;
import com.andinaseguros.usecases.port.out.repository.RenovacionRepository;
import java.util.List;

public class ListarRenovacionesUseCase {

    private final RenovacionRepository renovacionRepository;

    public ListarRenovacionesUseCase(RenovacionRepository renovacionRepository) {
        this.renovacionRepository = renovacionRepository;
    }

    public List<RenovacionResponse> execute() {
        return renovacionRepository.listar().stream()
                .map(propuesta -> toResponse(propuesta))
                .toList();
    }
}
