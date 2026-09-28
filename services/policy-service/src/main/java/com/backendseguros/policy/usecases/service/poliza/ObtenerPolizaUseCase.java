package com.backendseguros.policy.usecases.service.poliza;

import static com.backendseguros.policy.usecases.mapper.PolizaResponseMapper.toResponse;

import com.backendseguros.policy.usecases.dto.Responses.PolizaResponse;
import com.backendseguros.policy.entities.exception.RecursoNoEncontradoException;
import com.backendseguros.policy.entities.model.Poliza;
import com.backendseguros.policy.usecases.port.out.repository.PolizaRepository;
import java.util.UUID;

public class ObtenerPolizaUseCase {

    private final PolizaRepository polizaRepository;

    public ObtenerPolizaUseCase(PolizaRepository polizaRepository) {
        this.polizaRepository = polizaRepository;
    }

    public PolizaResponse execute(UUID polizaId) {
        Poliza poliza =
                polizaRepository
                        .buscarPorId(polizaId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Póliza"));

        return toResponse(poliza);
    }
}
