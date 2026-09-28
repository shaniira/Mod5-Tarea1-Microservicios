package com.backendseguros.identity.usecases.service.cliente;

import com.backendseguros.identity.entities.model.CorreoCliente;
import com.backendseguros.identity.usecases.port.out.repository.CorreoClienteRepository;
import java.util.UUID;

/**
 * Mantiene customer_email_index con customer.registered.v1 y customer.updated.v1 (paso 2.6). Los
 * eventos repetidos o desordenados se descartan por la versión del cliente, así que no hace falta
 * una colección inbox: aplicar dos veces el mismo evento no cambia nada.
 */
public class ActualizarIndiceCorreoClienteUseCase {
    public enum Resultado {
        APLICADO,
        VERSION_ANTIGUA
    }

    private final CorreoClienteRepository correos;

    public ActualizarIndiceCorreoClienteUseCase(CorreoClienteRepository correos) {
        this.correos = correos;
    }

    public Resultado execute(UUID clienteId, String correo, long version) {
        return correos.guardarSiEsMasNuevo(new CorreoCliente(clienteId, correo, version))
                ? Resultado.APLICADO
                : Resultado.VERSION_ANTIGUA;
    }
}
