package com.andinaseguros.quotation.frameworksdrivers.config;

import com.andinaseguros.quotation.interfaceadapters.out.event.RabbitMqProperties;
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
 * Colas propias de quotation (cada consumidor es dueño de sus colas), cada una con su DLQ: un fallo
 * se reintenta 3 veces y luego va a la DLQ.
 *
 * <ul>
 *   <li>quotation.customer.events: customer.registered/updated.v1 y vehicle.registered.v1
 *       (proyecciones customer_ref y vehicle_ref).
 *   <li>quotation.policy.events: policy.issued.v1 y policy.issuance-rejected.v1, de andina.events.
 *       Desde el paso 6.11 ya no se enlaza al exchange heredado andina.insurance.events.
 * </ul>
 *
 * Además declara andina.events, donde quotation publica quote.accepted.v1.
 */
@Configuration
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfiguration {
    private static final Logger log = LoggerFactory.getLogger(RabbitMqConfiguration.class);

    @Bean
    Declarables quotationTopology(
            RabbitMqProperties properties,
            @Value("${app.rabbitmq.dead-letter-exchange}") String dlxName,
            @Value("${app.rabbitmq.customer.queue}") String customerQueueName,
            @Value("${app.rabbitmq.customer.dead-letter-queue}") String customerDlqName,
            @Value("${app.rabbitmq.policy.queue}") String policyQueueName,
            @Value("${app.rabbitmq.policy.dead-letter-queue}") String policyDlqName) {
        TopicExchange events = new TopicExchange(properties.eventsExchange(), true, false);
        TopicExchange dlx = new TopicExchange(dlxName, true, false);
        Queue customer = cola(customerQueueName, dlxName, customerDlqName);
        Queue customerDlq = QueueBuilder.durable(customerDlqName).build();
        Queue policy = cola(policyQueueName, dlxName, policyDlqName);
        Queue policyDlq = QueueBuilder.durable(policyDlqName).build();
        return new Declarables(
                events,
                dlx,
                customer,
                customerDlq,
                policy,
                policyDlq,
                BindingBuilder.bind(customer).to(events).with("customer.registered.v1"),
                BindingBuilder.bind(customer).to(events).with("customer.updated.v1"),
                BindingBuilder.bind(customer).to(events).with("vehicle.registered.v1"),
                BindingBuilder.bind(policy).to(events).with("policy.issued.v1"),
                // Fase 6: compensación de la saga de emisión.
                BindingBuilder.bind(policy).to(events).with("policy.issuance-rejected.v1"),
                BindingBuilder.bind(customerDlq).to(dlx).with(customerDlqName),
                BindingBuilder.bind(policyDlq).to(dlx).with(policyDlqName));
    }

    private static Queue cola(String nombre, String dlxName, String dlqName) {
        return QueueBuilder.durable(nombre).deadLetterExchange(dlxName).deadLetterRoutingKey(dlqName).build();
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
