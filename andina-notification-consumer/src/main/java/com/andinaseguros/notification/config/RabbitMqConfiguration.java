package com.andinaseguros.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;

@Configuration
public class RabbitMqConfiguration {
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
        return QueueBuilder.durable(properties.queue())
                .deadLetterExchange(properties.deadLetterExchange())
                .deadLetterRoutingKey(properties.routingKey() + ".dlq")
                .build();
    }

    @Bean
    Queue notificationDeadLetterQueue(RabbitMqProperties properties) {
        return QueueBuilder.durable(properties.deadLetterQueue()).build();
    }

    @Bean
    Binding notificationBinding(
            Queue notificationQueue,
            TopicExchange eventsExchange,
            RabbitMqProperties properties) {
        return BindingBuilder.bind(notificationQueue)
                .to(eventsExchange)
                .with(properties.routingKey());
    }

    @Bean
    Binding notificationDeadLetterBinding(
            Queue notificationDeadLetterQueue,
            TopicExchange deadLetterExchange,
            RabbitMqProperties properties) {
        return BindingBuilder.bind(notificationDeadLetterQueue)
                .to(deadLetterExchange)
                .with(properties.routingKey() + ".dlq");
    }

    @Bean
    MessageConverter messageConverter(ObjectMapper objectMapper) {
        ObjectMapper rabbitObjectMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(rabbitObjectMapper);
    }

    @Bean
    RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(3);
        factory.setAdviceChain(
                RetryInterceptorBuilder.stateless()
                        .maxAttempts(4)
                        .backOffOptions(1000, 2.0, 10000)
                        .recoverer(new RejectAndDontRequeueRecoverer())
                        .build());
        return factory;
    }
}
