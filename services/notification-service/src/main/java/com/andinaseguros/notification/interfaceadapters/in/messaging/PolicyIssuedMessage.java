package com.andinaseguros.notification.interfaceadapters.in.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

/** Contrato policy.issued.v1 (contracts/events/policy.issued.v1.schema.json). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyIssuedMessage(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID aggregateId,
        String correlationId,
        String producer,
        PolicyIssuedData data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PolicyIssuedData(
            UUID policyId, UUID quoteId, UUID customerId, String policyNumber) {}
}
