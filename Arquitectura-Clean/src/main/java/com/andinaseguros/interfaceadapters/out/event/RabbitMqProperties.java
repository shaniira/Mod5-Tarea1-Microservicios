package com.andinaseguros.interfaceadapters.out.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * exchange: el exchange heredado (andina.insurance.events), donde sigue saliendo policy.issued.v1
 * para no romper a sus consumidores. eventsExchange: el exchange objetivo de la migración
 * (andina.events), donde salen los eventos nuevos (customer.*).
 *
 * <p>Las colas de notificación ya no se declaran aquí: desde la fase 1 son de notification-service
 * (cada consumidor es dueño de sus colas).
 */
@ConfigurationProperties("app.rabbitmq")
public record RabbitMqProperties(
        String exchange, String eventsExchange, String routingKey, String auditQueue) {}
