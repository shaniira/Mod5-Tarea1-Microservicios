package com.andinaseguros.notification.interfaceadapters.in.messaging;

import com.andinaseguros.notification.usecases.dto.ContactoClienteCommand;
import com.andinaseguros.notification.usecases.exception.EventoInvalidoException;
import com.andinaseguros.notification.usecases.service.ActualizarContactoClienteUseCase;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

/**
 * Consume customer.registered.v1 y customer.updated.v1 para mantener customer_contacts. Un solo
 * consumidor (concurrency 1) para aplicar en orden los eventos de un mismo cliente; la versión del
 * agregado cubre el desorden que pueda quedar (reintentos, backfill).
 */
public class CustomerEventsListener {
    public static final String LISTENER_ID = "customerEventsListener";
    private static final Logger log = LoggerFactory.getLogger(CustomerEventsListener.class);
    private static final Set<String> TIPOS = Set.of("CustomerRegistered", "CustomerUpdated");

    private final ActualizarContactoClienteUseCase actualizarContacto;

    public CustomerEventsListener(ActualizarContactoClienteUseCase actualizarContacto) {
        this.actualizarContacto = actualizarContacto;
    }

    @RabbitListener(id = LISTENER_ID, queues = "${app.rabbitmq.customer.queue}", concurrency = "1")
    public void consume(CustomerEventMessage event, Message raw) {
        try (MessageContext ignored = MessageContext.open(raw, event.correlationId(), event.eventId())) {
            if (!TIPOS.contains(event.eventType()) || event.eventVersion() != 1 || event.data() == null) {
                throw new EventoInvalidoException(
                        "Evento de cliente no soportado: " + event.eventType() + " v" + event.eventVersion());
            }
            var resultado =
                    actualizarContacto.execute(
                            new ContactoClienteCommand(
                                    event.eventId(),
                                    event.eventType(),
                                    event.data().customerId(),
                                    event.data().fullName(),
                                    event.data().email(),
                                    event.data().phone(),
                                    event.aggregateVersion() == null ? 0 : event.aggregateVersion()));
            log.info(
                    "{} cliente {} version {}: {}",
                    event.eventType(),
                    event.data().customerId(),
                    event.aggregateVersion(),
                    resultado);
        }
    }
}
