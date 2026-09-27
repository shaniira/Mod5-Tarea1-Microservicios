package com.andinaseguros.claims.interfaceadapters.out.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Sobre estándar de los eventos (contracts/events) con los datos de claim.*. */
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

    public record ClaimRegisteredData(
            UUID claimId,
            UUID policyId,
            UUID customerId,
            String status,
            boolean open,
            LocalDate date,
            String type,
            BigDecimal estimatedAmount,
            String currency,
            boolean insuredResponsible,
            String severity) {}

    public record ClaimStatusChangedData(
            UUID claimId,
            UUID policyId,
            String previousStatus,
            String newStatus,
            boolean open,
            boolean insuredResponsible) {}
}
