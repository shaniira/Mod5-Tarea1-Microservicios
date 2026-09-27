package com.andinaseguros.customer.interfaceadapters.out.event;

import com.andinaseguros.customer.entities.event.DomainEvent;
import com.andinaseguros.customer.interfaceadapters.out.event.IntegrationEventMapper.OutboundEvent;
import com.andinaseguros.customer.interfaceadapters.out.persistence.mongodb.document.OutboxEventDocument;
import com.andinaseguros.customer.interfaceadapters.out.persistence.mongodb.repository.SpringDataOutboxMongoRepository;
import com.andinaseguros.customer.usecases.port.out.event.DomainEventPublisherPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Supplier;
import org.slf4j.MDC;

/**
 * "Publicar" un evento es guardarlo en el Outbox. Si el caso de uso corre dentro de una
 * transacción, el evento se confirma junto con el cambio de negocio; {@link OutboxRelay} lo envía
 * después a RabbitMQ.
 */
public class OutboxDomainEventPublisherAdapter implements DomainEventPublisherPort {
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private final SpringDataOutboxMongoRepository outbox;
    private final IntegrationEventMapper mapper;
    private final ObjectMapper objectMapper;
    private final Supplier<String> traceparentActual;

    public OutboxDomainEventPublisherAdapter(
            SpringDataOutboxMongoRepository outbox,
            IntegrationEventMapper mapper,
            ObjectMapper objectMapper,
            Supplier<String> traceparentActual) {
        this.outbox = outbox;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.traceparentActual = traceparentActual;
    }

    @Override
    public void publicar(DomainEvent event) {
        OutboundEvent outbound = mapper.map(event, MDC.get(CORRELATION_ID_MDC_KEY));
        IntegrationEventMessage message = outbound.message();

        OutboxEventDocument document = new OutboxEventDocument();
        document.id = message.eventId().toString();
        document.exchange = outbound.exchange();
        document.routingKey = outbound.routingKey();
        document.eventType = message.eventType();
        document.aggregateId = message.aggregateId().toString();
        document.payload = toJson(message);
        document.correlationId = message.correlationId();
        document.traceparent = traceparentActual.get();
        document.status = OutboxEventDocument.PENDING;
        document.attempts = 0;
        document.createdAt = event.occurredAt();
        outbox.insert(document);
    }

    private String toJson(IntegrationEventMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el evento", exception);
        }
    }
}
