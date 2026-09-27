package com.andinaseguros.quotation.usecases.port.out.repository;

import com.andinaseguros.quotation.entities.model.ClienteRef;
import com.andinaseguros.quotation.entities.model.VehiculoRef;
import java.util.Optional;
import java.util.UUID;

/**
 * Proyecciones customer_ref y vehicle_ref (paso 5.2): reemplazan a ClienteRepository y
 * VehiculoRepository del monolito. Se escriben solo con eventos, con la carga inicial o con lo que
 * trae la lectura de refuerzo.
 */
public interface ReferenciaClientesRepository {
    Optional<ClienteRef> buscarCliente(UUID clienteId);

    Optional<VehiculoRef> buscarVehiculo(UUID vehiculoId);

    /**
     * Guarda si la versión es mayor que la guardada. Si la entrada no trae fecha de nacimiento
     * (eventos del backend) se conserva la que ya había. Devuelve false si era igual o más vieja.
     */
    boolean guardarClienteSiEsMasNuevo(ClienteRef cliente, long version);

    /**
     * Completa la fecha de nacimiento de una referencia que no la tiene (llegó por un evento del
     * backend) sin cambiar su versión: el siguiente evento la sigue pudiendo reemplazar.
     */
    void completarFechaNacimiento(ClienteRef cliente);

    /** Mismo criterio de versión para los vehículos. */
    boolean guardarVehiculoSiEsMasNuevo(VehiculoRef vehiculo, long version);
}
