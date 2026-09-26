package com.andinaseguros.usecases.service.poliza;

import static com.andinaseguros.usecases.mapper.PolizaResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.PolizaResponse;
import com.andinaseguros.entities.enums.EstadoPoliza;
import com.andinaseguros.usecases.port.out.repository.PolizaRepository;
import java.util.List;

public class ListarPolizasUseCase {

    private final PolizaRepository polizaRepository;

    public ListarPolizasUseCase(PolizaRepository polizaRepository) {
        this.polizaRepository = polizaRepository;
    }

    public List<PolizaResponse> execute() {
        return execute(null);
    }

    public List<PolizaResponse> execute(EstadoPoliza estado) {
        return polizaRepository.listar().stream()
                .filter(poliza -> estado == null || poliza.getEstado() == estado)
                .map(poliza -> toResponse(poliza))
                .toList();
    }
}
