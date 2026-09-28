package com.andinaseguros.notification.frameworksdrivers.config;

import com.andinaseguros.notification.frameworksdrivers.config.RabbitMqProperties.QueueSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología propia de notification-service. Cada consumidor es dueño de sus colas (sección 4 de la
 * propuesta): el backend ya no las declara.
 *
 * <ul>
 *   <li>{@code andina.policy.notification.queue}: policy.issued.v1 desde andina.events. Conserva el
 *       nombre y los argumentos de antes (incluida su DLX andina.insurance.events.dlx): RabbitMQ no
 *       deja cambiar los argumentos de una cola existente.
 *   <li>{@code notification.customer.events}: customer.registered.v1 y customer.updated.v1.
 * </ul>
 *
 * <p>Paso 6.11: ya no se enlaza al exchange heredado andina.insurance.events.
 */
@Configuration
public class RabbitMqConfiguration {

    @Bean
    Declarables notificationTopology(RabbitMqProperties properties) {
        TopicExchange events = new TopicExchange(properties.eventsExchange(), true, false);

        List<Declarable> declarables = new ArrayList<>();
        declarables.add(events);
        agregarCola(declarables, properties.policy(), List.of(events));
        agregarCola(declarables, properties.customer(), List.of(events));
        return new Declarables(declarables);
    }

    private void agregarCola(
            List<Declarable> declarables,
            QueueSettings settings,
            List<TopicExchange> origenes) {
        TopicExchange deadLetterExchange = new TopicExchange(settings.deadLetterExchange(), true, false);
        Queue queue =
                QueueBuilder.durable(settings.queue())
                        .deadLetterExchange(settings.deadLetterExchange())
                        .deadLetterRoutingKey(settings.deadLetterRoutingKey())
                        .build();
        Queue deadLetterQueue = QueueBuilder.durable(settings.deadLetterQueue()).build();

        declarables.add(deadLetterExchange);
        declarables.add(queue);
        declarables.add(deadLetterQueue);
        for (TopicExchange origen : origenes) {
            for (String routingKey : settings.routingKeys()) {
                declarables.add(BindingBuilder.bind(queue).to(origen).with(routingKey));
            }
        }
        Binding dlqBinding =
                BindingBuilder.bind(deadLetterQueue)
                        .to(deadLetterExchange)
                        .with(settings.deadLetterRoutingKey());
        declarables.add(dlqBinding);
    }

    @Bean
    MessageConverter messageConverter(ObjectMapper objectMapper) {
        // El ObjectMapper de Spring Boot ya trae JavaTimeModule e ignora campos desconocidos, así
        // que los campos nuevos y opcionales del sobre no rompen el consumo.
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            RabbitMqProperties properties) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setObservationEnabled(true);
        RabbitMqProperties.Retry retry = properties.retry();
        factory.setAdviceChain(
                RetryInterceptorBuilder.stateless()
                        .retryPolicy(ManejoFallosMensajes.politicaDeReintentos(retry.maxAttempts()))
                        .backOffOptions(
                                retry.initialIntervalMs(), retry.multiplier(), retry.maxIntervalMs())
                        .recoverer(new ManejoFallosMensajes.Recuperador())
                        .build());
        return factory;
    }
}
