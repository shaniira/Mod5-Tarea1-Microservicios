package com.backendseguros.notification.interfaceadapters.in.messaging;

import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;

/**
 * Restaura el correlationId que viene del publicador (header X-Correlation-Id o campo del sobre) en
 * el MDC, para que cada línea de log de este mensaje lo lleve.
 */
final class MessageContext implements AutoCloseable {
    static final String CORRELATION_HEADER = "X-Correlation-Id";
    static final String CORRELATION_MDC_KEY = "correlationId";
    static final String EVENT_MDC_KEY = "eventId";

    private MessageContext() {}

    static MessageContext open(Message raw, String envelopeCorrelationId, UUID eventId) {
        Object header = raw.getMessageProperties().getHeader(CORRELATION_HEADER);
        String correlationId =
                header != null
                        ? header.toString()
                        : envelopeCorrelationId != null
                                ? envelopeCorrelationId
                                : UUID.randomUUID().toString();
        MDC.put(CORRELATION_MDC_KEY, correlationId);
        if (eventId != null) {
            MDC.put(EVENT_MDC_KEY, eventId.toString());
        }
        return new MessageContext();
    }

    @Override
    public void close() {
        MDC.remove(CORRELATION_MDC_KEY);
        MDC.remove(EVENT_MDC_KEY);
    }
}
