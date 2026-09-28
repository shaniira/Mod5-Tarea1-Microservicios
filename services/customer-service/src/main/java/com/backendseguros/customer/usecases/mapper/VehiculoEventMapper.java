package com.backendseguros.customer.usecases.mapper;

import com.backendseguros.customer.entities.event.VehiculoRegistradoEvent;
import com.backendseguros.customer.entities.model.Vehiculo;
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
