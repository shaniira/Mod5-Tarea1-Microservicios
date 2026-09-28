package com.backendseguros.claims.interfaceadapters.out.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * eventsExchange: andina.events, donde salen claim.* y llegan policy.*. El exchange heredado
 * (andina.insurance.events) se retiró en el paso 6.11.
 */
@ConfigurationProperties("app.rabbitmq")
public record RabbitMqProperties(String eventsExchange) {}
