package com.andinaseguros.customer.usecases.service.cliente;

import static com.andinaseguros.customer.usecases.mapper.ClienteResponseMapper.toResponse;

import com.andinaseguros.customer.usecases.dto.Responses.ClienteResponse;
import com.andinaseguros.customer.usecases.port.out.repository.ClienteRepository;
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
