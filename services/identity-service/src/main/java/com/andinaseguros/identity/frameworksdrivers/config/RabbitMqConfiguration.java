package com.andinaseguros.identity.frameworksdrivers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cola propia de identity para customer.* (cada consumidor es dueño de sus colas): se enlaza al
 * exchange andina.events, con su DLQ. Los fallos se reintentan 3 veces y luego van a la DLQ.
 */
@Configuration
public class RabbitMqConfiguration {

    @Bean
    Declarables identityTopology(
            @Value("${app.rabbitmq.events-exchange}") String eventsExchange,
            @Value("${app.rabbitmq.customer.queue}") String queueName,
            @Value("${app.rabbitmq.customer.dead-letter-exchange}") String dlxName,
            @Value("${app.rabbitmq.customer.dead-letter-queue}") String dlqName) {
        TopicExchange events = new TopicExchange(eventsExchange, true, false);
        TopicExchange dlx = new TopicExchange(dlxName, true, false);
        Queue queue =
                QueueBuilder.durable(queueName)
                        .deadLetterExchange(dlxName)
                        .deadLetterRoutingKey(dlqName)
                        .build();
        Queue dlq = QueueBuilder.durable(dlqName).build();
        return new Declarables(
                events,
                dlx,
                queue,
                dlq,
                BindingBuilder.bind(queue).to(events).with("customer.registered.v1"),
                BindingBuilder.bind(queue).to(events).with("customer.updated.v1"),
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
}
