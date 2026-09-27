package com.andinaseguros.policy.usecases.service.poliza;

import static com.andinaseguros.policy.usecases.mapper.PolizaResponseMapper.toResponse;

import com.andinaseguros.policy.usecases.dto.Responses.PolizaResponse;
import com.andinaseguros.policy.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.policy.entities.model.Poliza;
import com.andinaseguros.policy.usecases.port.out.repository.PolizaRepository;
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
