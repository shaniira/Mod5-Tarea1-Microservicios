package com.andinaseguros.notification.frameworksdrivers.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * eventsExchange: andina.events, el único exchange de eventos desde el paso 6.11 (el heredado
 * andina.insurance.events se retiró junto con el monolito, que era su único productor).
 */
@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitMqProperties(
        String eventsExchange,
        QueueSettings policy,
        QueueSettings customer,
        Retry retry) {

    /**
     * @param routingKeys claves que escucha la cola
     * @param deadLetterExchange exchange al que van los mensajes rechazados
     * @param deadLetterRoutingKey clave con la que llegan a la DLQ
     */
    public record QueueSettings(
            String queue,
            List<String> routingKeys,
            String deadLetterExchange,
            String deadLetterRoutingKey,
            String deadLetterQueue) {}

    public record Retry(int maxAttempts, long initialIntervalMs, double multiplier, long maxIntervalMs) {}
}
