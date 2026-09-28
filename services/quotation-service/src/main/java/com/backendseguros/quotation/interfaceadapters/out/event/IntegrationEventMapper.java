package com.backendseguros.quotation.interfaceadapters.out.event;

import com.backendseguros.quotation.entities.event.CotizacionAceptadaEvent;
import com.backendseguros.quotation.entities.event.DomainEvent;
import com.backendseguros.quotation.interfaceadapters.out.event.IntegrationEventMessage.QuoteAcceptedData;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Traduce un evento de dominio al mensaje del catálogo, con su exchange y routing key. */
public class IntegrationEventMapper {
    public static final String PRODUCER = "quotation-service";
    /** Una cotización se acepta una sola vez (ver quote.accepted.v1). */
    static final long VERSION_ACEPTACION = 1;

    public record OutboundEvent(
            String exchange, String routingKey, IntegrationEventMessage message) {}

    private final RabbitMqProperties properties;

    public IntegrationEventMapper(RabbitMqProperties properties) {
        this.properties = properties;
    }

    public OutboundEvent map(DomainEvent event, String correlationId) {
        if (!(event instanceof CotizacionAceptadaEvent e)) {
            throw new IllegalArgumentException("Evento de dominio no soportado: " + event.getClass());
        }
        return new OutboundEvent(
                properties.eventsExchange(),
                "quote.accepted.v1",
                new IntegrationEventMessage(
                        e.eventId(),
                        "QuoteAccepted",
                        1,
                        e.occurredAt(),
                        e.cotizacionId(),
                        VERSION_ACEPTACION,
                        correlationId,
                        PRODUCER,
                        new QuoteAcceptedData(
                                e.cotizacionId(),
                                e.numero(),
                                e.clienteId(),
                                e.vehiculoId(),
                                e.prima(),
                                e.moneda(),
                                e.tablaTarifariaId(),
                                enUtc(e.creada()),
                                enUtc(e.expira()),
                                e.desglose())));
    }

    /**
     * Fase 7 (encontrado por la prueba de contrato): createdAt y expiresAt salían sin zona
     * ("2026-10-12T10:00:00"), y el contrato pide date-time RFC 3339. El dominio usa hora local
     * del servicio; se publica el mismo instante en UTC ("...Z"). policy-service lo sigue leyendo.
     */
    private static OffsetDateTime enUtc(LocalDateTime local) {
        return local == null ? null : local.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
    }
}
