package com.andinaseguros.notification.contact;

import java.util.UUID;

public record ClienteContact(UUID clienteId, String nombre, String correo, String telefono) {}
