package com.backendseguros.quotation.interfaceadapters.in.messaging;

import com.backendseguros.quotation.entities.enums.TipoUso;
import com.backendseguros.quotation.entities.enums.TipoVehiculo;
import com.backendseguros.quotation.entities.model.ClienteRef;
import com.backendseguros.quotation.entities.model.VehiculoRef;
import com.backendseguros.quotation.usecases.service.referencia.ActualizarReferenciasUseCase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

/**
 * Consume customer.registered/updated.v1 y vehicle.registered.v1 (contracts/events) para mantener
 * customer_ref y vehicle_ref (paso 5.2). Un solo consumidor para respetar el orden de cada
 * agregado. Un evento inválido va a la DLQ sin reintentos.
 */
public class CustomerEventsListener {
    private static final Logger log = LoggerFactory.getLogger(CustomerEventsListener.class);

    private final ActualizarReferenciasUseCase actualizar;

    public CustomerEventsListener(ActualizarReferenciasUseCase actualizar) {
        this.actualizar = actualizar;
    }

    @RabbitListener(id = "quotationCustomerEvents", queues = "${app.rabbitmq.customer.queue}", concurrency = "1")
    public void consume(EventMessage event, Message raw) {
        Object header = raw.getMessageProperties().getHeader("X-Correlation-Id");
        MDC.put("correlationId", header != null ? header.toString() : String.valueOf(event.correlationId()));
        try {
            if (event.eventVersion() != 1 || event.data() == null || event.aggregateVersion() == null) {
                throw rechazo(event);
            }
            var data = event.data();
            var resultado =
                    switch (event.eventType()) {
                        case "CustomerRegistered", "CustomerUpdated" -> {
                            if (data.customerId() == null) throw rechazo(event);
                            yield actualizar.cliente(
                                    new ClienteRef(data.customerId(), data.birthDate(), data.active()),
                                    event.aggregateVersion());
                        }
                        case "VehicleRegistered" -> {
                            if (data.vehicleId() == null || data.customerId() == null) throw rechazo(event);
                            yield actualizar.vehiculo(
                                    new VehiculoRef(
                                            data.vehicleId(),
                                            data.customerId(),
                                            TipoVehiculo.valueOf(data.vehicleType()),
                                            TipoUso.valueOf(data.usage()),
                                            data.manufactureYear()),
                                    event.aggregateVersion());
                        }
                        default -> throw rechazo(event);
                    };
            log.info("{} {} version {}: {}", event.eventType(), event.aggregateId(), event.aggregateVersion(), resultado);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private static AmqpRejectAndDontRequeueException rechazo(EventMessage event) {
        return new AmqpRejectAndDontRequeueException(
                "Evento no soportado: " + event.eventType() + " v" + event.eventVersion());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventMessage(
            UUID eventId,
            String eventType,
            int eventVersion,
            UUID aggregateId,
            Long aggregateVersion,
            String correlationId,
            EventData data) {}

    /** Campos de customer.* y de vehicle.registered.v1 (cada evento trae los suyos). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventData(
            UUID customerId,
            boolean active,
            LocalDate birthDate,
            UUID vehicleId,
            String vehicleType,
            String usage,
            int manufactureYear) {}
}
