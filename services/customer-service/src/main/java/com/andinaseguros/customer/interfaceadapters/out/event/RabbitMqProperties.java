package com.andinaseguros.customer.interfaceadapters.out.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * eventsExchange: el exchange objetivo de la migración (andina.events), donde salen customer.* y
 * vehicle.registered. customer-service solo publica: no declara colas (cada consumidor es dueño de
 * las suyas).
 */
@ConfigurationProperties("app.rabbitmq")
public record RabbitMqProperties(String eventsExchange) {}
