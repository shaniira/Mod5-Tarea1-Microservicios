package com.andinaseguros.claims.interfaceadapters.out.event;

import com.andinaseguros.claims.entities.event.DomainEvent;
import com.andinaseguros.claims.entities.event.SiniestroEstadoCambiadoEvent;
import com.andinaseguros.claims.entities.event.SiniestroRegistradoEvent;
import com.andinaseguros.claims.interfaceadapters.out.event.IntegrationEventMessage.ClaimRegisteredData;
import com.andinaseguros.claims.interfaceadapters.out.event.IntegrationEventMessage.ClaimStatusChangedData;

/** Traduce un evento de dominio al mensaje del catálogo, con su exchange y routing key. */
public class IntegrationEventMapper {
    public static final String PRODUCER = "claims-service";

    public record OutboundEvent(
            String exchange, String routingKey, IntegrationEventMessage message) {}

    private final RabbitMqProperties properties;

    public IntegrationEventMapper(RabbitMqProperties properties) {
        this.properties = properties;
    }

    public OutboundEvent map(DomainEvent event, String correlationId) {
        return switch (event) {
            case SiniestroRegistradoEvent e ->
                    new OutboundEvent(
                            properties.eventsExchange(),
                            "claim.registered.v1",
                            new IntegrationEventMessage(
                                    e.eventId(),
                                    "ClaimRegistered",
                                    1,
                                    e.occurredAt(),
                                    e.siniestroId(),
                                    e.version(),
                                    correlationId,
                                    PRODUCER,
                                    new ClaimRegisteredData(
                                            e.siniestroId(),
                                            e.polizaId(),
                                            e.clienteId(),
                                            e.estado().name(),
                                            e.abierto(),
                                            e.fecha(),
                                            e.tipo(),
                                            e.montoEstimado(),
                                            e.moneda(),
                                            e.responsabilidadAsegurado(),
                                            e.gravedad())));
            case SiniestroEstadoCambiadoEvent e ->
                    new OutboundEvent(
                            properties.eventsExchange(),
                            "claim.status-changed.v1",
                            new IntegrationEventMessage(
                                    e.eventId(),
                                    "ClaimStatusChanged",
                                    1,
                                    e.occurredAt(),
                                    e.siniestroId(),
                                    e.version(),
                                    correlationId,
                                    PRODUCER,
                                    new ClaimStatusChangedData(
                                            e.siniestroId(),
                                            e.polizaId(),
                                            e.estadoAnterior().name(),
                                            e.estadoNuevo().name(),
                                            e.abierto(),
                                            e.responsabilidadAsegurado())));
            default ->
                    throw new IllegalArgumentException(
                            "Evento de dominio no soportado: " + event.getClass());
        };
    }
}
