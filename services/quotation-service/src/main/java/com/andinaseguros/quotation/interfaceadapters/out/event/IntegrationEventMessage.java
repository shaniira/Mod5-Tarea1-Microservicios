package com.andinaseguros.quotation.interfaceadapters.out.event;

import com.andinaseguros.quotation.entities.model.ResultadoTarificacion;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Sobre estándar de los eventos (contracts/events) con los datos de quote.accepted.v1. */
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

    public record QuoteAcceptedData(
            UUID quoteId,
            String quoteNumber,
            UUID customerId,
            UUID vehicleId,
            BigDecimal premium,
            String currency,
            UUID tariffTableId,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt,
            ResultadoTarificacion breakdown) {}
}
