package com.backendseguros.quotation.entities.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Copia mínima de un cliente (proyección customer_ref, paso 5.2): lo que la tarificación necesita.
 * La edad se calcula igual que Cliente.edad() del monolito, para que el resultado de la
 * tarificación sea idéntico (paso 5.7).
 */
public record ClienteRef(UUID clienteId, LocalDate fechaNacimiento, boolean activo) {
    public ClienteRef {
        if (clienteId == null) {
            throw new IllegalArgumentException("La referencia necesita el id del cliente");
        }
    }

    /** Sin fecha de nacimiento no se puede tarificar (los eventos del backend no la traen). */
    public boolean completo() {
        return fechaNacimiento != null;
    }

    public int edad() {
        return LocalDate.now().getYear()
                - fechaNacimiento.getYear()
                - (LocalDate.now().getDayOfYear() < fechaNacimiento.getDayOfYear() ? 1 : 0);
    }
}
