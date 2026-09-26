package com.andinaseguros.usecases.service.cliente;

import static com.andinaseguros.usecases.mapper.ClienteResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.ClienteResponse;
import com.andinaseguros.usecases.port.out.repository.ClienteRepository;
import java.util.List;

public class ListarClientesUseCase {

    private final ClienteRepository clienteRepository;

    public ListarClientesUseCase(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<ClienteResponse> execute() {
        return clienteRepository.listar().stream().map(cliente -> toResponse(cliente)).toList();
    }
}
