package com.andinaseguros.identity.interfaceadapters.in.messaging;

import com.andinaseguros.identity.usecases.service.cliente.ActualizarIndiceCorreoClienteUseCase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

/**
 * Consume customer.registered.v1 y customer.updated.v1 (contracts/events) para mantener
 * customer_email_index (paso 2.6). Un solo consumidor para respetar el orden de cada cliente.
 */
public class CustomerEventsListener {
    private static final Logger log = LoggerFactory.getLogger(CustomerEventsListener.class);
    private static final Set<String> TIPOS = Set.of("CustomerRegistered", "CustomerUpdated");

    private final ActualizarIndiceCorreoClienteUseCase actualizarIndice;

    public CustomerEventsListener(ActualizarIndiceCorreoClienteUseCase actualizarIndice) {
        this.actualizarIndice = actualizarIndice;
    }

    @RabbitListener(id = "identityCustomerEvents", queues = "${app.rabbitmq.customer.queue}", concurrency = "1")
    public void consume(CustomerEventMessage event, Message raw) {
        Object header = raw.getMessageProperties().getHeader("X-Correlation-Id");
        MDC.put("correlationId", header != null ? header.toString() : String.valueOf(event.correlationId()));
        try {
            if (!TIPOS.contains(event.eventType())
                    || event.eventVersion() != 1
                    || event.data() == null
                    || event.data().customerId() == null
                    || event.aggregateVersion() == null) {
                throw new AmqpRejectAndDontRequeueException(
                        "Evento de cliente no soportado: " + event.eventType() + " v" + event.eventVersion());
            }
            var resultado =
                    actualizarIndice.execute(
                            event.data().customerId(), event.data().email(), event.aggregateVersion());
            log.info(
                    "{} cliente {} version {}: {}",
                    event.eventType(),
                    event.data().customerId(),
                    event.aggregateVersion(),
                    resultado);
        } finally {
            MDC.remove("correlationId");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomerEventMessage(
            UUID eventId,
            String eventType,
            int eventVersion,
            UUID aggregateId,
            Long aggregateVersion,
            String correlationId,
            CustomerData data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomerData(UUID customerId, String email) {}
}
