package com.andinaseguros.usecases.service.renovacion;

import static com.andinaseguros.usecases.mapper.RenovacionResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.RenovacionResponse;
import com.andinaseguros.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.usecases.port.out.repository.PolizaRepository;
import com.andinaseguros.usecases.port.out.repository.RenovacionRepository;
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
