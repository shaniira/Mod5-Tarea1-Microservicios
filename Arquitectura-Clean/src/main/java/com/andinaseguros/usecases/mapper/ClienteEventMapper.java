package com.andinaseguros.usecases.mapper;

import com.andinaseguros.entities.event.ClienteActualizadoEvent;
import com.andinaseguros.entities.event.ClienteRegistradoEvent;
import com.andinaseguros.entities.model.Cliente;
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
                cliente.getCorreo(),
                cliente.getTelefono(),
                cliente.isActivo(),
                cliente.getVersion());
    }
}
