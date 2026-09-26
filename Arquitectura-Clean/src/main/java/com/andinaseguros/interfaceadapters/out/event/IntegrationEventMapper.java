package com.andinaseguros.interfaceadapters.out.event;

import com.andinaseguros.entities.event.ClienteActualizadoEvent;
import com.andinaseguros.entities.event.ClienteRegistradoEvent;
import com.andinaseguros.entities.event.DomainEvent;
import com.andinaseguros.entities.event.PolizaEmitidaEvent;
import com.andinaseguros.interfaceadapters.out.event.IntegrationEventMessage.CustomerData;
import com.andinaseguros.interfaceadapters.out.event.IntegrationEventMessage.PolicyIssuedData;

/** Traduce un evento de dominio al mensaje del catálogo, con su exchange y routing key. */
public class IntegrationEventMapper {
    public static final String PRODUCER = "andina-backend";

    public record OutboundEvent(
            String exchange, String routingKey, IntegrationEventMessage message) {}

    private final RabbitMqProperties properties;

    public IntegrationEventMapper(RabbitMqProperties properties) {
        this.properties = properties;
    }

    public OutboundEvent map(DomainEvent event, String correlationId) {
        return switch (event) {
            case PolizaEmitidaEvent e ->
                    new OutboundEvent(
                            properties.exchange(),
                            properties.routingKey(),
                            new IntegrationEventMessage(
                                    e.eventId(),
                                    "PolicyIssued",
                                    1,
                                    e.occurredAt(),
                                    e.polizaId(),
                                    null,
                                    correlationId,
                                    PRODUCER,
                                    new PolicyIssuedData(
                                            e.polizaId(),
                                            e.cotizacionId(),
                                            e.clienteId(),
                                            e.numeroPoliza())));
            case ClienteRegistradoEvent e ->
                    new OutboundEvent(
                            properties.eventsExchange(),
                            "customer.registered.v1",
                            new IntegrationEventMessage(
                                    e.eventId(),
                                    "CustomerRegistered",
                                    1,
                                    e.occurredAt(),
                                    e.clienteId(),
                                    e.version(),
                                    correlationId,
                                    PRODUCER,
                                    new CustomerData(
                                            e.clienteId(),
                                            e.tipoDocumento(),
                                            e.numeroDocumento(),
                                            e.nombres(),
                                            e.apellidos(),
                                            nombreCompleto(e.nombres(), e.apellidos()),
                                            e.correo(),
                                            e.telefono(),
                                            e.activo())));
            case ClienteActualizadoEvent e ->
                    new OutboundEvent(
                            properties.eventsExchange(),
                            "customer.updated.v1",
                            new IntegrationEventMessage(
                                    e.eventId(),
                                    "CustomerUpdated",
                                    1,
                                    e.occurredAt(),
                                    e.clienteId(),
                                    e.version(),
                                    correlationId,
                                    PRODUCER,
                                    new CustomerData(
                                            e.clienteId(),
                                            e.tipoDocumento(),
                                            e.numeroDocumento(),
                                            e.nombres(),
                                            e.apellidos(),
                                            nombreCompleto(e.nombres(), e.apellidos()),
                                            e.correo(),
                                            e.telefono(),
                                            e.activo())));
            default ->
                    throw new IllegalArgumentException(
                            "Evento de dominio no soportado: " + event.getClass());
        };
    }

    private static String nombreCompleto(String nombres, String apellidos) {
        return ((nombres == null ? "" : nombres) + " " + (apellidos == null ? "" : apellidos))
                .trim();
    }
}
