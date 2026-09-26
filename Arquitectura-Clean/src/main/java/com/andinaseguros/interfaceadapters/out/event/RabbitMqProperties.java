package com.andinaseguros.interfaceadapters.out.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.rabbitmq")
public record RabbitMqProperties(
        String exchange,
        String deadLetterExchange,
        String routingKey,
        String notificationQueue,
        String notificationDeadLetterQueue,
        String auditQueue) {}
