package com.backendseguros.customer.usecases.service.cliente;

import static com.backendseguros.customer.usecases.mapper.ClienteResponseMapper.toResponse;

import com.backendseguros.customer.usecases.dto.Responses.ClienteResponse;
import com.backendseguros.customer.usecases.port.out.repository.ClienteRepository;
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
