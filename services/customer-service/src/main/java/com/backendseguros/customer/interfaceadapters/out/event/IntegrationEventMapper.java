package com.backendseguros.customer.interfaceadapters.out.event;

import com.backendseguros.customer.entities.event.ClienteActualizadoEvent;
import com.backendseguros.customer.entities.event.ClienteRegistradoEvent;
import com.backendseguros.customer.entities.event.DomainEvent;
import com.backendseguros.customer.entities.event.VehiculoRegistradoEvent;
import com.backendseguros.customer.interfaceadapters.out.event.IntegrationEventMessage.CustomerData;
import com.backendseguros.customer.interfaceadapters.out.event.IntegrationEventMessage.VehicleData;
import java.time.Instant;
import java.util.UUID;

/** Traduce un evento de dominio al mensaje del catálogo, con su exchange y routing key. */
public class IntegrationEventMapper {
    public static final String PRODUCER = "customer-service";
    /** Un vehículo no se modifica después de registrarse (ver vehicle.registered.v1). */
    static final long VERSION_VEHICULO = 1;

    public record OutboundEvent(
            String exchange, String routingKey, IntegrationEventMessage message) {}

    private final RabbitMqProperties properties;

    public IntegrationEventMapper(RabbitMqProperties properties) {
        this.properties = properties;
    }

    public OutboundEvent map(DomainEvent event, String correlationId) {
        return switch (event) {
            case ClienteRegistradoEvent e ->
                    cliente(
                            "customer.registered.v1",
                            "CustomerRegistered",
                            e.eventId(),
                            e.occurredAt(),
                            e.version(),
                            correlationId,
                            new CustomerData(
                                    e.clienteId(),
                                    e.tipoDocumento(),
                                    e.numeroDocumento(),
                                    e.nombres(),
                                    e.apellidos(),
                                    nombreCompleto(e.nombres(), e.apellidos()),
                                    e.correo(),
                                    e.telefono(),
                                    e.activo(),
                                    e.fechaNacimiento()));
            case ClienteActualizadoEvent e ->
                    cliente(
                            "customer.updated.v1",
                            "CustomerUpdated",
                            e.eventId(),
                            e.occurredAt(),
                            e.version(),
                            correlationId,
                            new CustomerData(
                                    e.clienteId(),
                                    e.tipoDocumento(),
                                    e.numeroDocumento(),
                                    e.nombres(),
                                    e.apellidos(),
                                    nombreCompleto(e.nombres(), e.apellidos()),
                                    e.correo(),
                                    e.telefono(),
                                    e.activo(),
                                    e.fechaNacimiento()));
            case VehiculoRegistradoEvent e ->
                    new OutboundEvent(
                            properties.eventsExchange(),
                            "vehicle.registered.v1",
                            new IntegrationEventMessage(
                                    e.eventId(),
                                    "VehicleRegistered",
                                    1,
                                    e.occurredAt(),
                                    e.vehiculoId(),
                                    VERSION_VEHICULO,
                                    correlationId,
                                    PRODUCER,
                                    new VehicleData(
                                            e.vehiculoId(),
                                            e.clienteId(),
                                            e.placa(),
                                            e.marca(),
                                            e.modelo(),
                                            e.anioFabricacion(),
                                            e.tipo().name(),
                                            e.uso().name(),
                                            e.zonaCirculacion())));
            default ->
                    throw new IllegalArgumentException(
                            "Evento de dominio no soportado: " + event.getClass());
        };
    }

    private OutboundEvent cliente(
            String routingKey,
            String eventType,
            UUID eventId,
            Instant occurredAt,
            long version,
            String correlationId,
            CustomerData data) {
        return new OutboundEvent(
                properties.eventsExchange(),
                routingKey,
                new IntegrationEventMessage(
                        eventId,
                        eventType,
                        1,
                        occurredAt,
                        data.customerId(),
                        version,
                        correlationId,
                        PRODUCER,
                        data));
    }

    private static String nombreCompleto(String nombres, String apellidos) {
        return ((nombres == null ? "" : nombres) + " " + (apellidos == null ? "" : apellidos))
                .trim();
    }
}
