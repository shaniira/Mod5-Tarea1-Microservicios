package com.backendseguros.notification.entities.model;

import java.util.UUID;

/**
 * Copia local, de solo lectura, de los datos de contacto de un cliente (proyección
 * customer_contacts). La fuente de verdad es el dueño de los clientes (hoy el backend, en la fase
 * 3 customer-service); esta copia se mantiene con los eventos customer.*.
 */
public record ContactoCliente(
        UUID clienteId, String nombre, String correo, String telefono, long version) {

    public ContactoCliente {
        if (clienteId == null) {
            throw new IllegalArgumentException("El contacto necesita el id del cliente");
        }
        if (version < 1) {
            throw new IllegalArgumentException("La version del cliente debe ser positiva");
        }
    }

    public boolean tieneTelefono() {
        return telefono != null && !telefono.isBlank();
    }
}
