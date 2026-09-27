package com.andinaseguros.quotation.usecases.port.out.cliente;

import com.andinaseguros.quotation.entities.model.ClienteRef;
import com.andinaseguros.quotation.entities.model.VehiculoRef;
import java.util.Optional;
import java.util.UUID;

/**
 * Lectura de refuerzo (read-through, paso 5.3) hacia el dueño de los clientes, solo cuando la
 * proyección no tiene el dato. Optional.empty() = el dueño respondió que no existe. Si el dueño no
 * responde, lanza ClientesNoDisponiblesException.
 */
public interface DirectorioClientesPort {
    Optional<ClienteRef> buscarCliente(UUID clienteId);

    Optional<VehiculoRef> buscarVehiculo(UUID clienteId, UUID vehiculoId);
}
