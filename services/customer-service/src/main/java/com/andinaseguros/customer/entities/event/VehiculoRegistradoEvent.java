package com.andinaseguros.customer.entities.event;

import com.andinaseguros.customer.entities.enums.TipoUso;
import com.andinaseguros.customer.entities.enums.TipoVehiculo;
import java.time.Instant;
import java.util.UUID;

/** vehicle.registered.v1: lo que necesita la tarificación (tipo, uso y año) y la propiedad. */
public record VehiculoRegistradoEvent(
        UUID eventId,
        Instant occurredAt,
        UUID vehiculoId,
        UUID clienteId,
        String placa,
        String marca,
        String modelo,
        int anioFabricacion,
        TipoVehiculo tipo,
        TipoUso uso,
        String zonaCirculacion)
        implements DomainEvent {}
