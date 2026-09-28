package com.backendseguros.customer.usecases.mapper;

import com.backendseguros.customer.entities.event.ClienteActualizadoEvent;
import com.backendseguros.customer.entities.event.ClienteRegistradoEvent;
import com.backendseguros.customer.entities.model.Cliente;
import java.time.Instant;
import java.util.UUID;

public final class ClienteEventMapper {
    private ClienteEventMapper() {}

    public static ClienteRegistradoEvent registrado(UUID eventId, Instant ahora, Cliente cliente) {
        return new ClienteRegistradoEvent(
                eventId,
                ahora,
                cliente.getId(),
                cliente.getTipoDocumento(),
                cliente.getNumeroDocumento(),
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getFechaNacimiento(),
                cliente.getCorreo(),
                cliente.getTelefono(),
                cliente.isActivo(),
                cliente.getVersion());
    }

    public static ClienteActualizadoEvent actualizado(UUID eventId, Instant ahora, Cliente cliente) {
        return new ClienteActualizadoEvent(
                eventId,
                ahora,
                cliente.getId(),
                cliente.getTipoDocumento(),
                cliente.getNumeroDocumento(),
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getFechaNacimiento(),
                cliente.getCorreo(),
                cliente.getTelefono(),
                cliente.isActivo(),
                cliente.getVersion());
    }
}
