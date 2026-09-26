package com.andinaseguros.usecases.service.vehiculo;

import static com.andinaseguros.usecases.mapper.VehiculoResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.VehiculoResponse;
import com.andinaseguros.usecases.port.out.repository.VehiculoRepository;
import java.util.List;
import java.util.UUID;

public class ListarVehiculosClienteUseCase {

    private final VehiculoRepository vehiculoRepository;

    public ListarVehiculosClienteUseCase(VehiculoRepository vehiculoRepository) {
        this.vehiculoRepository = vehiculoRepository;
    }

    public List<VehiculoResponse> execute(UUID clienteId) {
        return vehiculoRepository.listarPorCliente(clienteId).stream()
                .map(vehiculo -> toResponse(vehiculo))
                .toList();
    }
}
