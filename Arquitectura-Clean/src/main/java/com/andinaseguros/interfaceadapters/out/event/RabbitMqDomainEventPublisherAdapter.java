package com.andinaseguros.interfaceadapters.out.event;

import com.andinaseguros.entities.event.DomainEvent;
import com.andinaseguros.entities.event.PolizaEmitidaEvent;
import com.andinaseguros.usecases.port.out.event.DomainEventPublisherPort;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class RabbitMqDomainEventPublisherAdapter implements DomainEventPublisherPort {
    private final RabbitTemplate rabbitTemplate;
    private final PolicyIssuedMessageMapper mapper;
    private final RabbitMqProperties properties;

    public RabbitMqDomainEventPublisherAdapter(
            RabbitTemplate rabbitTemplate,
            PolicyIssuedMessageMapper mapper,
            RabbitMqProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    public void publicar(DomainEvent event) {
        if (!(event instanceof PolizaEmitidaEvent policyIssued)) {
            throw new IllegalArgumentException("Evento de dominio no soportado: " + event.getClass());
        }
        PolicyIssuedMessage message = mapper.toMessage(policyIssued);
        rabbitTemplate.convertAndSend(
                properties.exchange(),
                properties.routingKey(),
                message,
                amqpMessage -> {
                    amqpMessage.getMessageProperties().setMessageId(message.eventId().toString());
                    amqpMessage.getMessageProperties().setType(message.eventType());
                    return amqpMessage;
                });
    }
}
