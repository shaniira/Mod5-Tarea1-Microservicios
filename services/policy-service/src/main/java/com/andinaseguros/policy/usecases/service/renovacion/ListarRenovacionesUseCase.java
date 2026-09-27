package com.andinaseguros.policy.usecases.service.renovacion;

import static com.andinaseguros.policy.usecases.mapper.RenovacionResponseMapper.toResponse;

import com.andinaseguros.policy.usecases.dto.Responses.RenovacionResponse;
import com.andinaseguros.policy.usecases.port.out.repository.RenovacionRepository;
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
