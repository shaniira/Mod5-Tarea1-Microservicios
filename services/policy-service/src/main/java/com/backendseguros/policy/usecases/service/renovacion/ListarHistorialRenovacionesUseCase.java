package com.backendseguros.policy.usecases.service.renovacion;

import static com.backendseguros.policy.usecases.mapper.RenovacionResponseMapper.toResponse;

import com.backendseguros.policy.usecases.dto.Responses.RenovacionResponse;
import com.backendseguros.policy.entities.exception.RecursoNoEncontradoException;
import com.backendseguros.policy.usecases.port.out.repository.PolizaRepository;
import com.backendseguros.policy.usecases.port.out.repository.RenovacionRepository;
import java.util.List;
import java.util.UUID;

public class ListarHistorialRenovacionesUseCase {

    private final PolizaRepository polizaRepository;
    private final RenovacionRepository renovacionRepository;

    public ListarHistorialRenovacionesUseCase(
            PolizaRepository polizaRepository, RenovacionRepository renovacionRepository) {
        this.polizaRepository = polizaRepository;
        this.renovacionRepository = renovacionRepository;
    }

    public List<RenovacionResponse> execute(UUID polizaId) {
        polizaRepository
                .buscarPorId(polizaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Póliza"));

        return renovacionRepository.listarPorPoliza(polizaId).stream()
                .map(propuesta -> toResponse(propuesta))
                .toList();
    }
}
