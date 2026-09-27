package com.andinaseguros.customer.usecases.port.out.vehicle;

import com.andinaseguros.customer.usecases.model.VehicleInformation;
import java.util.Optional;

/**
 * Caché de las placas ya consultadas en el proveedor externo (paso 3.2, TTL 24 h). Si la caché no
 * responde, se comporta como vacía: nunca impide la consulta.
 */
public interface VehicleInformationCachePort {
    Optional<VehicleInformation> buscar(String placa);

    void guardar(VehicleInformation informacion);
}
