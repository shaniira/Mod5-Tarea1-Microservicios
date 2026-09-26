package com.andinaseguros.usecases.service.cliente;

import static com.andinaseguros.usecases.mapper.ClienteResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.CrearClienteRequestModel;
import com.andinaseguros.usecases.dto.Responses.ClienteResponse;
import com.andinaseguros.usecases.port.in.RegistrarClienteUseCase;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.model.Cliente;
import com.andinaseguros.usecases.port.out.repository.ClienteRepository;
import java.util.UUID;

public class CrearClienteUseCase implements RegistrarClienteUseCase {

    private final ClienteRepository clienteRepository;

    public CrearClienteUseCase(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public ClienteResponse execute(CrearClienteRequestModel solicitud) {
        boolean documentoRegistrado =
                clienteRepository.buscarPorDocumento(solicitud.numeroDocumento()).isPresent();

        if (documentoRegistrado) {
            throw new ReglaNegocioException(
                    "DOCUMENTO_DUPLICADO", "Ya existe un cliente con ese documento");
        }

        Cliente cliente =
                new Cliente(
                        UUID.randomUUID(),
                        solicitud.tipoDocumento(),
                        solicitud.numeroDocumento(),
                        solicitud.nombres(),
                        solicitud.apellidos(),
                        solicitud.fechaNacimiento(),
                        solicitud.correo(),
                        solicitud.telefono(),
                        true);

        Cliente clienteGuardado = clienteRepository.guardar(cliente);

        return toResponse(clienteGuardado);
    }
}
