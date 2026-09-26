package com.andinaseguros.identity.usecases.port.out.repository;

import com.andinaseguros.identity.entities.model.CorreoCliente;
import java.util.Optional;
import java.util.UUID;

/** Índice customer_email_index (paso 2.6): reemplaza la consulta a la colección clientes. */
public interface CorreoClienteRepository {
    Optional<UUID> buscarClientePorCorreo(String correo);

    /** Guarda solo si es más nuevo que lo que hay; false si era igual o más viejo. */
    boolean guardarSiEsMasNuevo(CorreoCliente entrada);
}
