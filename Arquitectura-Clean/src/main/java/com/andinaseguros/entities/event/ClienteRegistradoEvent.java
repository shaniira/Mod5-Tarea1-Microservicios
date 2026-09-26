package com.andinaseguros.entities.event;

import java.time.Instant;
import java.util.UUID;

public record ClienteRegistradoEvent(
        UUID eventId,
        Instant occurredAt,
        UUID clienteId,
        String tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        boolean activo,
        long version)
        implements DomainEvent {}
