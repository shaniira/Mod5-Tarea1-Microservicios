package com.backendseguros.policy.usecases.service.poliza;

import static com.backendseguros.policy.usecases.mapper.PolizaResponseMapper.toResponse;

import com.backendseguros.policy.usecases.dto.Responses.PolizaResponse;
import com.backendseguros.policy.entities.enums.EstadoPoliza;
import com.backendseguros.policy.usecases.port.out.repository.PolizaRepository;
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
