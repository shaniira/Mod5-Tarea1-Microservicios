package com.andinaseguros.usecases.service.siniestro;

import static com.andinaseguros.usecases.mapper.SiniestroResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.SiniestroResponse;
import com.andinaseguros.usecases.port.out.repository.SiniestroRepository;
import java.util.List;
import java.util.UUID;

public class ListarSiniestrosUseCase {

    private final SiniestroRepository siniestroRepository;

    public ListarSiniestrosUseCase(SiniestroRepository siniestroRepository) {
        this.siniestroRepository = siniestroRepository;
    }

    public List<SiniestroResponse> execute(UUID polizaId) {
        return siniestroRepository.listarPorPoliza(polizaId).stream()
                .map(siniestro -> toResponse(siniestro))
                .toList();
    }
}
