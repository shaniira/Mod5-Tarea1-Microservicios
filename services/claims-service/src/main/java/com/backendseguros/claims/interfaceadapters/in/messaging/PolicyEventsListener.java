package com.backendseguros.claims.interfaceadapters.in.messaging;

import com.backendseguros.claims.entities.enums.EstadoPoliza;
import com.backendseguros.claims.usecases.service.poliza.ActualizarEstadoPolizaUseCase;
import com.backendseguros.claims.usecases.service.poliza.RegistrarPolizaEmitidaUseCase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

/**
 * Consume policy.* (contracts/events) para mantener policy_ref (paso 4.3): policy.issued.v1 del
 * backend (exchange heredado) o de policy-service (andina.events), y desde la fase 6
 * policy.renewed.v1, policy.expired.v1 y policy.cancelled.v1. Un evento inválido va a la DLQ sin
 * reintentos.
 */
public class PolicyEventsListener {
    private static final Logger log = LoggerFactory.getLogger(PolicyEventsListener.class);

    private final RegistrarPolizaEmitidaUseCase registrarPoliza;
    private final ActualizarEstadoPolizaUseCase actualizarEstado;

    public PolicyEventsListener(
            RegistrarPolizaEmitidaUseCase registrarPoliza, ActualizarEstadoPolizaUseCase actualizarEstado) {
        this.registrarPoliza = registrarPoliza;
        this.actualizarEstado = actualizarEstado;
    }

    @RabbitListener(id = "claimsPolicyEvents", queues = "${app.rabbitmq.policy.queue}", concurrency = "1")
    public void consume(PolicyEventMessage event, Message raw) {
        Object header = raw.getMessageProperties().getHeader("X-Correlation-Id");
        MDC.put("correlationId", header != null ? header.toString() : String.valueOf(event.correlationId()));
        try {
            var d = event.data();
            if (event.eventVersion() != 1 || d == null) {
                throw rechazo(event);
            }
            Object resultado =
                    switch (event.eventType()) {
                        case "PolicyIssued" -> {
                            if (d.policyId() == null) throw rechazo(event);
                            yield registrarPoliza.execute(d.policyId(), d.customerId(), d.policyNumber());
                        }
                        case "PolicyRenewed" -> {
                            if (d.previousPolicyId() == null || d.newPolicyId() == null || event.aggregateVersion() == null)
                                throw rechazo(event);
                            yield actualizarEstado.renovada(
                                    d.previousPolicyId(), event.aggregateVersion(), d.newPolicyId(), d.customerId(),
                                    d.newPolicyNumber());
                        }
                        case "PolicyExpired", "PolicyCancelled" -> {
                            if (d.policyId() == null || event.aggregateVersion() == null) throw rechazo(event);
                            yield actualizarEstado.cambiarEstado(
                                    d.policyId(),
                                    "PolicyExpired".equals(event.eventType()) ? EstadoPoliza.VENCIDA : EstadoPoliza.CANCELADA,
                                    event.aggregateVersion());
                        }
                        default -> throw rechazo(event);
                    };
            log.info("{} póliza {}: {}", event.eventType(), event.aggregateId(), resultado);
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
            UUID eventId,
            String eventType,
            int eventVersion,
            UUID aggregateId,
            Long aggregateVersion,
            String correlationId,
            PolicyData data) {}

    /** Campos de policy.issued, policy.renewed y policy.expired/cancelled (cada uno trae los suyos). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PolicyData(
            UUID policyId,
            UUID customerId,
            String policyNumber,
            UUID newPolicyId,
            UUID previousPolicyId,
            String newPolicyNumber) {}
}
