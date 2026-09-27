package com.andinaseguros.customer.usecases.service.cliente;

import static com.andinaseguros.customer.usecases.mapper.ClienteResponseMapper.toResponse;

import com.andinaseguros.customer.usecases.dto.Responses.ClienteResponse;
import com.andinaseguros.customer.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.customer.entities.model.Cliente;
import com.andinaseguros.customer.usecases.port.out.repository.ClienteRepository;
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
