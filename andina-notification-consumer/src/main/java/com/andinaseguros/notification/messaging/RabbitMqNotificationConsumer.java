package com.andinaseguros.notification.messaging;

import com.andinaseguros.notification.config.RabbitMqProperties;
import com.andinaseguros.notification.contract.PolicyIssuedMessage;
import com.andinaseguros.notification.idempotency.IdempotencyService;
import com.andinaseguros.notification.service.PolicyNotificationService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqNotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(RabbitMqNotificationConsumer.class);
    private final PolicyNotificationService notificationService;
    private final IdempotencyService idempotencyService;

    public RabbitMqNotificationConsumer(
            PolicyNotificationService notificationService, IdempotencyService idempotencyService) {
        this.notificationService = notificationService;
        this.idempotencyService = idempotencyService;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void consume(PolicyIssuedMessage event, Message rawMessage, Channel channel)
            throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        if (idempotencyService.wasProcessed(event.eventId())) {
            channel.basicAck(deliveryTag, false);
            log.info("Evento duplicado confirmado sin repetir envio: {}", event.eventId());
            return;
        }

        notificationService.notifyPolicyIssued(event);
        idempotencyService.markProcessed(event.eventId());
        channel.basicAck(deliveryTag, false);
        log.info("Evento procesado y confirmado: {}", event.eventId());
    }
}
