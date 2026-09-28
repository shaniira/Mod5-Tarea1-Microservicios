package com.backendseguros.quotation.interfaceadapters.in.messaging;

import com.backendseguros.quotation.usecases.service.cotizacion.MarcarCotizacionEmitidaUseCase;
import com.backendseguros.quotation.usecases.service.cotizacion.RegistrarRechazoEmisionUseCase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

/**
 * Consume policy.issued.v1 (paso 5.4) para marcar la cotización como EMITIDA. La cola escucha el
 * exchange heredado (donde publica el backend) y andina.events (donde publicará policy-service).
 */
public class PolicyEventsListener {
    private static final Logger log = LoggerFactory.getLogger(PolicyEventsListener.class);

    private final MarcarCotizacionEmitidaUseCase marcarEmitida;
    private final RegistrarRechazoEmisionUseCase registrarRechazo;

    public PolicyEventsListener(
            MarcarCotizacionEmitidaUseCase marcarEmitida, RegistrarRechazoEmisionUseCase registrarRechazo) {
        this.marcarEmitida = marcarEmitida;
        this.registrarRechazo = registrarRechazo;
    }

    /** policy.issued.v1 marca EMITIDA; policy.issuance-rejected.v1 (fase 6) registra el motivo. */
    @RabbitListener(id = "quotationPolicyEvents", queues = "${app.rabbitmq.policy.queue}", concurrency = "1")
    public void consume(PolicyEventMessage event, Message raw) {
        Object header = raw.getMessageProperties().getHeader("X-Correlation-Id");
        MDC.put("correlationId", header != null ? header.toString() : String.valueOf(event.correlationId()));
        try {
            var d = event.data();
            if (event.eventVersion() != 1 || d == null || d.quoteId() == null) {
                throw rechazo(event);
            }
            Object resultado =
                    switch (event.eventType()) {
                        case "PolicyIssued" -> marcarEmitida.execute(d.quoteId());
                        case "PolicyIssuanceRejected" ->
                                registrarRechazo.execute(event.eventId(), d.quoteId(), d.reasonCode(), d.reason());
                        default -> throw rechazo(event);
                    };
            log.info("{} cotización {}: {}", event.eventType(), d.quoteId(), resultado);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private static AmqpRejectAndDontRequeueException rechazo(PolicyEventMessage event) {
        return new AmqpRejectAndDontRequeueException(
                "Evento de póliza no soportado: " + event.eventType() + " v" + event.eventVersion());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PolicyEventMessage(
            UUID eventId, String eventType, int eventVersion, String correlationId, PolicyData data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PolicyData(UUID policyId, UUID quoteId, String reasonCode, String reason) {}
}
