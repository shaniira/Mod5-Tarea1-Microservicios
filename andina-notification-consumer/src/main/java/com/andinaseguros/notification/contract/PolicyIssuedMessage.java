package com.andinaseguros.notification.contract;

import java.time.Instant;
import java.util.UUID;

public record PolicyIssuedMessage(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID aggregateId,
        PolicyIssuedData data) {
    public record PolicyIssuedData(
            UUID policyId,
            UUID quoteId,
            UUID customerId,
            String policyNumber) {}
}
