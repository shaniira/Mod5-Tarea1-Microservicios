package com.andinaseguros.notification.interfaceadapters.in.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

/**
 * Contrato customer.registered.v1 y customer.updated.v1 (mismo sobre y mismos datos; ver
 * contracts/events). Solo se leen los campos que necesita notification-service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerEventMessage(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID aggregateId,
        Long aggregateVersion,
        String correlationId,
        String producer,
        CustomerData data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomerData(UUID customerId, String fullName, String email, String phone) {}
}
