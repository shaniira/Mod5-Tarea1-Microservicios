package com.andinaseguros.policy.frameworksdrivers.config;

import com.andinaseguros.policy.interfaceadapters.out.event.RabbitMqProperties;
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
 * Colas propias de policy-service (cada consumidor es dueño de sus colas), cada una con su DLQ: un
 * fallo se reintenta 3 veces y luego va a la DLQ.
 *
 * <ul>
 *   <li>policy.quote.events: quote.accepted.v1 (proyección accepted_quotes).
 *   <li>policy.claim.events: claim.registered.v1 y claim.status-changed.v1 (proyección claim_ref).
 * </ul>
 *
 * Además declara andina.events, donde policy-service publica policy.*.
 */
@Configuration
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfiguration {
    private static final Logger log = LoggerFactory.getLogger(RabbitMqConfiguration.class);

    @Bean
    Declarables policyTopology(
            RabbitMqProperties properties,
            @Value("${app.rabbitmq.dead-letter-exchange}") String dlxName,
            @Value("${app.rabbitmq.quote.queue}") String quoteQueueName,
            @Value("${app.rabbitmq.quote.dead-letter-queue}") String quoteDlqName,
            @Value("${app.rabbitmq.claim.queue}") String claimQueueName,
            @Value("${app.rabbitmq.claim.dead-letter-queue}") String claimDlqName) {
        TopicExchange events = new TopicExchange(properties.eventsExchange(), true, false);
        TopicExchange dlx = new TopicExchange(dlxName, true, false);
        Queue quote = cola(quoteQueueName, dlxName, quoteDlqName);
        Queue quoteDlq = QueueBuilder.durable(quoteDlqName).build();
        Queue claim = cola(claimQueueName, dlxName, claimDlqName);
        Queue claimDlq = QueueBuilder.durable(claimDlqName).build();
        return new Declarables(
                events,
                dlx,
                quote,
                quoteDlq,
                claim,
                claimDlq,
                BindingBuilder.bind(quote).to(events).with("quote.accepted.v1"),
                BindingBuilder.bind(claim).to(events).with("claim.registered.v1"),
                BindingBuilder.bind(claim).to(events).with("claim.status-changed.v1"),
                BindingBuilder.bind(quoteDlq).to(dlx).with(quoteDlqName),
                BindingBuilder.bind(claimDlq).to(dlx).with(claimDlqName));
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
