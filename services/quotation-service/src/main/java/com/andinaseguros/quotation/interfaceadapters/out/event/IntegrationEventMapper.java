package com.andinaseguros.quotation.interfaceadapters.out.event;

import com.andinaseguros.quotation.entities.event.CotizacionAceptadaEvent;
import com.andinaseguros.quotation.entities.event.DomainEvent;
import com.andinaseguros.quotation.interfaceadapters.out.event.IntegrationEventMessage.QuoteAcceptedData;

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
                                e.creada(),
                                e.expira(),
                                e.desglose())));
    }
}
