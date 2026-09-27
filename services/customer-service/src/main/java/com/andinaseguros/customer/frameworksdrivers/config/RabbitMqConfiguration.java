package com.andinaseguros.customer.frameworksdrivers.config;

import com.andinaseguros.customer.interfaceadapters.out.event.RabbitMqProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * customer-service solo publica: declara el exchange andina.events y nada más. Las colas las
 * declara cada consumidor (notification, identity, backend).
 */
@Configuration
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfiguration {
    private static final Logger log = LoggerFactory.getLogger(RabbitMqConfiguration.class);

    @Bean
    TopicExchange andinaEventsExchange(RabbitMqProperties properties) {
        return new TopicExchange(properties.eventsExchange(), true, false);
    }

    /** mandatory + publisher confirms: el relay del Outbox solo marca como enviado lo confirmado. */
    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        // Sin observación automática: el traceparent lo pone el relay con el de la petición que
        // originó el evento (guardado en el Outbox), no el del hilo del relay.
        template.setReturnsCallback(
                returned ->
                        log.warn(
                                "Mensaje no enrutable devuelto por RabbitMQ. exchange={}, routingKey={}, replyText={}",
                                returned.getExchange(),
                                returned.getRoutingKey(),
                                returned.getReplyText()));
        return template;
    }
}
