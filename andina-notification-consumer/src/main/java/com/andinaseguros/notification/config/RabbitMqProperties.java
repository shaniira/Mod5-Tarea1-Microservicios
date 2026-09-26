package com.andinaseguros.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitMqProperties(
        String exchange,
        String deadLetterExchange,
        String routingKey,
        String queue,
        String deadLetterQueue) {}
