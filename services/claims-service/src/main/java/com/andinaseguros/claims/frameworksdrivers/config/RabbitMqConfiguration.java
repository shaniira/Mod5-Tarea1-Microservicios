package com.andinaseguros.claims.frameworksdrivers.config;

import com.andinaseguros.claims.interfaceadapters.out.event.RabbitMqProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cola propia de claims para policy.* (cada consumidor es dueño de sus colas). policy.issued.v1
 * llega por los dos exchanges durante la transición: andina.insurance.events (backend) y
 * andina.events (policy-service). policy.renewed/expired/cancelled.v1 solo los publica
 * policy-service, en andina.events. Con su DLQ: un fallo se reintenta 3 veces y luego va a la DLQ.
 * Además declara andina.events, donde claims publica claim.*.
 */
@Configuration
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfiguration {
    private static final Logger log = LoggerFactory.getLogger(RabbitMqConfiguration.class);

    @Bean
    Declarables claimsTopology(
            RabbitMqProperties properties,
            @Value("${app.rabbitmq.policy.queue}") String queueName,
            @Value("${app.rabbitmq.policy.dead-letter-exchange}") String dlxName,
            @Value("${app.rabbitmq.policy.dead-letter-queue}") String dlqName) {
        TopicExchange events = new TopicExchange(properties.eventsExchange(), true, false);
        TopicExchange insurance = new TopicExchange(properties.insuranceExchange(), true, false);
        TopicExchange dlx = new TopicExchange(dlxName, true, false);
        Queue queue =
                QueueBuilder.durable(queueName)
                        .deadLetterExchange(dlxName)
                        .deadLetterRoutingKey(dlqName)
                        .build();
        Queue dlq = QueueBuilder.durable(dlqName).build();
        return new Declarables(
                events,
                insurance,
                dlx,
                queue,
                dlq,
                BindingBuilder.bind(queue).to(insurance).with("policy.issued.v1"),
                BindingBuilder.bind(queue).to(events).with("policy.issued.v1"),
                BindingBuilder.bind(queue).to(events).with("policy.renewed.v1"),
                BindingBuilder.bind(queue).to(events).with("policy.expired.v1"),
                BindingBuilder.bind(queue).to(events).with("policy.cancelled.v1"),
                BindingBuilder.bind(dlq).to(dlx).with(dlqName));
    }

    @Bean
    MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setObservationEnabled(true);
        factory.setAdviceChain(
                RetryInterceptorBuilder.stateless()
                        .maxAttempts(3)
                        .backOffOptions(1000, 2.0, 5000)
                        .recoverer(new RejectAndDontRequeueRecoverer())
                        .build());
        return factory;
    }

    /** mandatory + publisher confirms: el relay del Outbox solo marca como enviado lo confirmado. */
    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        // Sin observación automática: el traceparent lo pone el relay con el de la petición que
        // originó el evento (guardado en el Outbox).
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
