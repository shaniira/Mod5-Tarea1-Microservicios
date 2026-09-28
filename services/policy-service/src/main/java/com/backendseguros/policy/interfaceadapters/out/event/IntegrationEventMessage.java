package com.backendseguros.policy.interfaceadapters.out.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Sobre estándar de los eventos (contracts/events) con los datos de policy.*. */
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

    /** Mismos cuatro campos que publicaba el backend, más los opcionales de la fase 6. */
    public record PolicyIssuedData(
            UUID policyId,
            UUID quoteId,
            UUID customerId,
            String policyNumber,
            UUID vehicleId,
            BigDecimal premium,
            String currency,
            LocalDate startDate,
            LocalDate endDate,
            String status) {}

    public record PolicyRenewedData(
            UUID newPolicyId,
            UUID previousPolicyId,
            UUID renewalId,
            UUID customerId,
            UUID vehicleId,
            String newPolicyNumber,
            BigDecimal premium,
            String currency,
            LocalDate startDate,
            LocalDate endDate) {}

    public record PolicyIssuanceRejectedData(
            UUID quoteId, String reasonCode, String reason, UUID existingPolicyId) {}
}
