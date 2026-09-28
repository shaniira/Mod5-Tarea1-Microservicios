package com.backendseguros.quotation.entities.model;

import com.backendseguros.quotation.entities.enums.TipoUso;
import com.backendseguros.quotation.entities.enums.TipoVehiculo;
import java.time.Year;
import java.util.UUID;

/**
 * Copia mínima de un vehículo (proyección vehicle_ref, paso 5.2): dueño, tipo, uso y año. La
 * antigüedad se calcula igual que Vehiculo.antiguedad() del monolito.
 */
public record VehiculoRef(
        UUID vehiculoId, UUID clienteId, TipoVehiculo tipo, TipoUso uso, int anioFabricacion) {
    public VehiculoRef {
        if (vehiculoId == null) {
            throw new IllegalArgumentException("La referencia necesita el id del vehículo");
        }
    }

    public int antiguedad() {
        return Year.now().getValue() - anioFabricacion;
    }
}
