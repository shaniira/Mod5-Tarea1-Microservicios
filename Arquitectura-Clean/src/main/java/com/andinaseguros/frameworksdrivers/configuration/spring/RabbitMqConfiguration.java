package com.andinaseguros.frameworksdrivers.configuration.spring;

import com.andinaseguros.interfaceadapters.out.event.RabbitMqProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfiguration {
    private static final Logger log = LoggerFactory.getLogger(RabbitMqConfiguration.class);

    // Fase 1: el backend solo declara los exchanges donde publica y la cola de auditoría. Las colas
    // de notificación (y su DLQ) pasaron a ser de notification-service.

    @Bean
    TopicExchange insuranceEventsExchange(RabbitMqProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    TopicExchange andinaEventsExchange(RabbitMqProperties properties) {
        return new TopicExchange(properties.eventsExchange(), true, false);
    }

    @Bean
    Queue auditQueue(RabbitMqProperties properties) {
        return QueueBuilder.durable(properties.auditQueue()).build();
    }

    @Bean
    Binding auditBinding(
            Queue auditQueue, TopicExchange insuranceEventsExchange, RabbitMqProperties properties) {
        return BindingBuilder.bind(auditQueue)
                .to(insuranceEventsExchange)
                .with(properties.routingKey());
    }

    @Bean
    MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(rabbitMessageConverter);
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.warn("RabbitMQ rechazo la confirmacion de publicacion. correlationData={}, cause={}",
                        correlationData, cause);
            }
        });
        template.setReturnsCallback(returned -> log.warn(
                "Mensaje no enrutable devuelto por RabbitMQ. exchange={}, routingKey={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        return template;
    }
}
