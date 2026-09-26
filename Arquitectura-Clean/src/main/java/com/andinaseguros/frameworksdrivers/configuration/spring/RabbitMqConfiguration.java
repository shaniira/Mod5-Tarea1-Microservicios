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

    @Bean
    TopicExchange eventsExchange(RabbitMqProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    TopicExchange deadLetterExchange(RabbitMqProperties properties) {
        return new TopicExchange(properties.deadLetterExchange(), true, false);
    }

    @Bean
    Queue notificationQueue(RabbitMqProperties properties) {
        return QueueBuilder.durable(properties.notificationQueue())
                .deadLetterExchange(properties.deadLetterExchange())
                .deadLetterRoutingKey(properties.routingKey() + ".dlq")
                .build();
    }

    @Bean
    Queue notificationDeadLetterQueue(RabbitMqProperties properties) {
        return QueueBuilder.durable(properties.notificationDeadLetterQueue()).build();
    }

    @Bean
    Queue auditQueue(RabbitMqProperties properties) {
        return QueueBuilder.durable(properties.auditQueue()).build();
    }

    @Bean
    Binding notificationBinding(Queue notificationQueue, TopicExchange eventsExchange, RabbitMqProperties properties) {
        return BindingBuilder.bind(notificationQueue).to(eventsExchange).with(properties.routingKey());
    }

    @Bean
    Binding auditBinding(Queue auditQueue, TopicExchange eventsExchange, RabbitMqProperties properties) {
        return BindingBuilder.bind(auditQueue).to(eventsExchange).with(properties.routingKey());
    }

    @Bean
    Binding deadLetterBinding(Queue notificationDeadLetterQueue, TopicExchange deadLetterExchange, RabbitMqProperties properties) {
        return BindingBuilder.bind(notificationDeadLetterQueue).to(deadLetterExchange).with(properties.routingKey() + ".dlq");
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
