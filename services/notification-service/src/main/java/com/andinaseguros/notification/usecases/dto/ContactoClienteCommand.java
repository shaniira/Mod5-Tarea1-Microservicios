package com.andinaseguros.notification.usecases.dto;

import java.util.UUID;

public record ContactoClienteCommand(
        UUID eventId,
        String tipoEvento,
        UUID clienteId,
        String nombre,
        String correo,
        String telefono,
        long version) {}
