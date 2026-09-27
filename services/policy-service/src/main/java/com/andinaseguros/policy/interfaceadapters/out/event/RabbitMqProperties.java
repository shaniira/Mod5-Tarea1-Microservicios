package com.andinaseguros.policy.interfaceadapters.out.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * eventsExchange: el exchange objetivo de la migración (andina.events), donde policy-service publica
 * policy.* y consume quote.accepted.v1 y claim.*. Los consumidores de policy.issued.v1 escuchan
 * también el exchange heredado, donde publica el backend; policy-service ya no lo usa.
 */
@ConfigurationProperties("app.rabbitmq")
public record RabbitMqProperties(String eventsExchange) {}
