package com.andinaseguros.customer.interfaceadapters.out.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Sobre estándar de los eventos (contracts/events). customer.* conserva exactamente los campos que
 * publicaba el backend y agrega birthDate (opcional), así que los consumidores de las fases 1 y 2
 * siguen leyéndolo sin cambios.
 */
public record IntegrationEventMessage(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID aggregateId,
        Long aggregateVersion,
        String correlationId,
        String producer,
        Object data) {

    public record CustomerData(
            UUID customerId,
            String documentType,
            String documentNumber,
            String names,
            String surnames,
            String fullName,
            String email,
            String phone,
            boolean active,
            LocalDate birthDate) {}

    public record VehicleData(
            UUID vehicleId,
            UUID customerId,
            String plate,
            String brand,
            String model,
            int manufactureYear,
            String vehicleType,
            String usage,
            String circulationZone) {}
}
