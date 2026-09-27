package com.andinaseguros.quotation.interfaceadapters.out.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * eventsExchange: el exchange objetivo de la migración (andina.events), donde sale quote.accepted.
 * insuranceExchange: el exchange heredado (andina.insurance.events), donde el backend sigue
 * publicando policy.issued.v1. Las colas de quotation-service escuchan los dos.
 */
@ConfigurationProperties("app.rabbitmq")
public record RabbitMqProperties(String eventsExchange, String insuranceExchange) {}
