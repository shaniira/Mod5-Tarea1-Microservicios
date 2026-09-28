package com.backendseguros.customer.usecases.service.cliente;

import static com.backendseguros.customer.usecases.mapper.ClienteResponseMapper.toResponse;

import com.backendseguros.customer.usecases.dto.Responses.ClienteResponse;
import com.backendseguros.customer.entities.exception.RecursoNoEncontradoException;
import com.backendseguros.customer.entities.model.Cliente;
import com.backendseguros.customer.usecases.port.out.repository.ClienteRepository;
import java.util.UUID;

public class ObtenerClienteUseCase {

    private final ClienteRepository clienteRepository;

    public ObtenerClienteUseCase(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public ClienteResponse execute(UUID clienteId) {
        Cliente cliente =
                clienteRepository
                        .buscarPorId(clienteId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Cliente"));

        return toResponse(cliente);
    }
}
