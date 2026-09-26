package com.andinaseguros.interfaceadapters.out.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Sobre estándar de todos los eventos (contracts/events). Conserva los campos que ya tenía
 * policy.issued.v1 y agrega aggregateVersion, correlationId y producer como campos opcionales, así
 * que los consumidores anteriores lo siguen leyendo.
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

    public record PolicyIssuedData(
            UUID policyId, UUID quoteId, UUID customerId, String policyNumber) {}

    public record CustomerData(
            UUID customerId,
            String documentType,
            String documentNumber,
            String names,
            String surnames,
            String fullName,
            String email,
            String phone,
            boolean active) {}
}
