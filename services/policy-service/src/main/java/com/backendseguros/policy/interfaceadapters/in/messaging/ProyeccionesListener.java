package com.backendseguros.policy.interfaceadapters.in.messaging;

import com.backendseguros.policy.entities.model.CotizacionAceptada;
import com.backendseguros.policy.entities.model.SiniestroRef;
import com.backendseguros.policy.entities.valueobject.Dinero;
import com.backendseguros.policy.usecases.service.referencia.ActualizarProyeccionesUseCase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

/**
 * Consume quote.accepted.v1 (accepted_quotes) y claim.registered/status-changed.v1 (claim_ref), paso
 * 6.2. Cada origen tiene su cola con DLQ; un solo consumidor por cola para respetar el orden. Un
 * evento inválido va a la DLQ sin reintentos.
 */
public class ProyeccionesListener {
    private static final Logger log = LoggerFactory.getLogger(ProyeccionesListener.class);

    private final ActualizarProyeccionesUseCase actualizar;

    public ProyeccionesListener(ActualizarProyeccionesUseCase actualizar) {
        this.actualizar = actualizar;
    }

    @RabbitListener(id = "policyQuoteEvents", queues = "${app.rabbitmq.quote.queue}", concurrency = "1")
    public void cotizacion(QuoteEvent event, Message raw) {
        conCorrelacion(raw, event.correlationId(), () -> {
            var d = event.data();
            // expiresAt es obligatorio en el contrato: sin él la cotización parecería no vencer nunca.
            if (!"QuoteAccepted".equals(event.eventType()) || event.eventVersion() != 1 || d == null
                    || d.quoteId() == null || d.customerId() == null || d.vehicleId() == null || d.premium() == null
                    || d.expiresAt() == null) {
                throw rechazo(event.eventType(), event.eventVersion());
            }
            var resultado =
                    actualizar.cotizacionAceptada(
                            new CotizacionAceptada(
                                    d.quoteId(), d.quoteNumber(), d.customerId(), d.vehicleId(),
                                    new Dinero(d.premium(), d.currency() == null ? "PEN" : d.currency()),
                                    d.expiresAt()));
            log.info("QuoteAccepted cotización {}: {}", d.quoteId(), resultado);
        });
    }

    @RabbitListener(id = "policyClaimEvents", queues = "${app.rabbitmq.claim.queue}", concurrency = "1")
    public void siniestro(ClaimEvent event, Message raw) {
        conCorrelacion(raw, event.correlationId(), () -> {
            var d = event.data();
            boolean tipoValido =
                    "ClaimRegistered".equals(event.eventType()) || "ClaimStatusChanged".equals(event.eventType());
            if (!tipoValido || event.eventVersion() != 1 || d == null || d.claimId() == null
                    || d.policyId() == null || d.open() == null || event.aggregateVersion() == null) {
                throw rechazo(event.eventType(), event.eventVersion());
            }
            var resultado =
                    actualizar.siniestro(
                            new SiniestroRef(d.claimId(), d.policyId(), d.open(), d.insuredResponsible()),
                            event.aggregateVersion());
            log.info("{} siniestro {} version {}: {}", event.eventType(), d.claimId(), event.aggregateVersion(), resultado);
        });
    }

    private static void conCorrelacion(Message raw, String correlationId, Runnable accion) {
        Object header = raw.getMessageProperties().getHeader("X-Correlation-Id");
        MDC.put("correlationId", header != null ? header.toString() : String.valueOf(correlationId));
        try {
            accion.run();
        } finally {
            MDC.remove("correlationId");
        }
    }

    private static AmqpRejectAndDontRequeueException rechazo(String tipo, int version) {
        return new AmqpRejectAndDontRequeueException("Evento no soportado: " + tipo + " v" + version);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuoteEvent(String eventType, int eventVersion, String correlationId, QuoteData data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuoteData(
            UUID quoteId,
            String quoteNumber,
            UUID customerId,
            UUID vehicleId,
            BigDecimal premium,
            String currency,
            LocalDateTime expiresAt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClaimEvent(
            String eventType, int eventVersion, Long aggregateVersion, String correlationId, ClaimData data) {}

    /** open: claim.registered y claim.status-changed lo traen (estado abierto o cerrado). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClaimData(UUID claimId, UUID policyId, Boolean open, boolean insuredResponsible) {}
}
