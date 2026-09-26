package com.andinaseguros.notification.usecases.port.out;

import com.andinaseguros.notification.entities.model.ContactoCliente;
import java.util.Optional;
import java.util.UUID;

public interface ContactoClienteRepository {
    Optional<ContactoCliente> buscar(UUID clienteId);

    /**
     * Guarda el contacto solo si no hay una copia con versión igual o mayor. Devuelve false si la
     * copia existente ya era igual o más nueva (evento repetido o desordenado).
     */
    boolean guardarSiEsMasNuevo(ContactoCliente contacto);
}
