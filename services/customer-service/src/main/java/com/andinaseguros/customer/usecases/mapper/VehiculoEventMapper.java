package com.andinaseguros.customer.usecases.mapper;

import com.andinaseguros.customer.entities.event.VehiculoRegistradoEvent;
import com.andinaseguros.customer.entities.model.Vehiculo;
import java.time.Instant;
import java.util.UUID;

public final class VehiculoEventMapper {
    private VehiculoEventMapper() {}

    public static VehiculoRegistradoEvent registrado(UUID eventId, Instant ahora, Vehiculo vehiculo) {
        return new VehiculoRegistradoEvent(
                eventId,
                ahora,
                vehiculo.getId(),
                vehiculo.getClienteId(),
                vehiculo.getPlaca().valor(),
                vehiculo.getMarca(),
                vehiculo.getModelo(),
                vehiculo.getAnioFabricacion(),
                vehiculo.getTipo(),
                vehiculo.getUso(),
                vehiculo.getZonaCirculacion());
    }
}
