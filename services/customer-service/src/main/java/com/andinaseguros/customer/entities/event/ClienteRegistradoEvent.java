package com.andinaseguros.customer.entities.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClienteRegistradoEvent(
        UUID eventId,
        Instant occurredAt,
        UUID clienteId,
        String tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String correo,
        String telefono,
        boolean activo,
        long version)
        implements DomainEvent {}
