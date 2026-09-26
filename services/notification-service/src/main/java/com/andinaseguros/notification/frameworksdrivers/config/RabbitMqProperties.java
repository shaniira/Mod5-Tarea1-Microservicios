package com.andinaseguros.notification.frameworksdrivers.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * legacyExchange / eventsExchange: durante la transición (fase 1 en adelante) las colas se enlazan
 * a los dos exchanges (sección 4 de la propuesta). Cuando todos publiquen en andina.events, el
 * heredado se retira.
 */
@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitMqProperties(
        String legacyExchange,
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
