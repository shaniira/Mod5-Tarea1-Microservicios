package com.andinaseguros.claims.interfaceadapters.out.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * eventsExchange: el exchange objetivo de la migración (andina.events), donde salen claim.*.
 * insuranceExchange: el exchange heredado (andina.insurance.events), donde el backend sigue
 * publicando policy.issued.v1. La cola de pólizas de claims-service escucha los dos.
 */
@ConfigurationProperties("app.rabbitmq")
public record RabbitMqProperties(String eventsExchange, String insuranceExchange) {}
