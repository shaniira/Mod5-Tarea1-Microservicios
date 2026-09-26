package com.andinaseguros.usecases.service.poliza;

import static com.andinaseguros.usecases.mapper.PolizaResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.PolizaResponse;
import com.andinaseguros.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.entities.model.Poliza;
import com.andinaseguros.usecases.port.out.repository.PolizaRepository;
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
