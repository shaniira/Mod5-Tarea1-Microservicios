package com.andinaseguros.notification.interfaceadapters.in.messaging;

import com.andinaseguros.notification.usecases.dto.PolizaEmitidaCommand;
import com.andinaseguros.notification.usecases.exception.EventoInvalidoException;
import com.andinaseguros.notification.usecases.service.NotificarPolizaEmitidaUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

/**
 * Consume policy.issued.v1. Este es el listener que se pausa cuando se abre el circuit breaker de
 * WhatsApp (ver PausaListenerPorCircuitoAbierto), por eso tiene un id fijo.
 */
public class PolicyIssuedListener {
    public static final String LISTENER_ID = "policyIssuedListener";
    private static final Logger log = LoggerFactory.getLogger(PolicyIssuedListener.class);

    private final NotificarPolizaEmitidaUseCase notificarPolizaEmitida;

    public PolicyIssuedListener(NotificarPolizaEmitidaUseCase notificarPolizaEmitida) {
        this.notificarPolizaEmitida = notificarPolizaEmitida;
    }

    @RabbitListener(
            id = LISTENER_ID,
            queues = "${app.rabbitmq.policy.queue}",
            concurrency = "${app.rabbitmq.policy.concurrency:1-3}")
    public void consume(PolicyIssuedMessage event, Message raw) {
        try (MessageContext ignored = MessageContext.open(raw, event.correlationId(), event.eventId())) {
            validar(event);
            var resultado =
                    notificarPolizaEmitida.execute(
                            new PolizaEmitidaCommand(
                                    event.eventId(),
                                    event.data().policyId(),
                                    event.data().customerId(),
                                    event.data().policyNumber()));
            log.info("PolicyIssued {}: {}", event.data().policyNumber(), resultado);
        }
    }

    private void validar(PolicyIssuedMessage event) {
        if (!"PolicyIssued".equals(event.eventType()) || event.eventVersion() != 1) {
            throw new EventoInvalidoException(
                    "Tipo o version no soportados: " + event.eventType() + " v" + event.eventVersion());
        }
        if (event.data() == null) {
            throw new EventoInvalidoException("PolicyIssued sin data");
        }
    }
}
