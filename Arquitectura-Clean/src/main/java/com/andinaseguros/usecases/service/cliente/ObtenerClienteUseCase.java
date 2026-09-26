package com.andinaseguros.usecases.service.cliente;

import static com.andinaseguros.usecases.mapper.ClienteResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.ClienteResponse;
import com.andinaseguros.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.entities.model.Cliente;
import com.andinaseguros.usecases.port.out.repository.ClienteRepository;
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
