package com.andinaseguros.identity.entities.model;

import java.util.Locale;
import java.util.UUID;

/**
 * Entrada del índice customer_email_index: qué cliente corresponde a un correo. Es una copia de
 * solo lectura mantenida con los eventos customer.*; la fuente de verdad sigue siendo el dueño de
 * los clientes.
 */
public record CorreoCliente(UUID clienteId, String correo, long version) {
    public CorreoCliente {
        if (clienteId == null) {
            throw new IllegalArgumentException("El indice necesita el id del cliente");
        }
        if (version < 1) {
            throw new IllegalArgumentException("La version del cliente debe ser positiva");
        }
        correo = normalizar(correo);
    }

    public static String normalizar(String correo) {
        return correo == null || correo.isBlank() ? null : correo.trim().toLowerCase(Locale.ROOT);
    }
}
