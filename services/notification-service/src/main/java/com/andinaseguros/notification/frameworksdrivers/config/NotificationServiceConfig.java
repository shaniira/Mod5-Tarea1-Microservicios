package com.andinaseguros.notification.frameworksdrivers.config;

import com.andinaseguros.notification.interfaceadapters.in.messaging.CustomerEventsListener;
import com.andinaseguros.notification.interfaceadapters.in.messaging.PolicyIssuedListener;
import com.andinaseguros.notification.interfaceadapters.out.persistence.mongodb.MongoContactoClienteRepository;
import com.andinaseguros.notification.interfaceadapters.out.persistence.mongodb.MongoInboxRepository;
import com.andinaseguros.notification.interfaceadapters.out.whatsapp.WhatsAppClient;
import com.andinaseguros.notification.interfaceadapters.out.whatsapp.WhatsAppNotificationAdapter;
import com.andinaseguros.notification.usecases.port.out.ContactoClienteRepository;
import com.andinaseguros.notification.usecases.port.out.InboxRepository;
import com.andinaseguros.notification.usecases.port.out.NotificacionPort;
import com.andinaseguros.notification.usecases.service.ActualizarContactoClienteUseCase;
import com.andinaseguros.notification.usecases.service.NotificarPolizaEmitidaUseCase;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
public class NotificationServiceConfig {
    static final String WHATSAPP = "whatsapp";

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ContactoClienteRepository contactoClienteRepository(MongoTemplate mongoTemplate, Clock clock) {
        return new MongoContactoClienteRepository(mongoTemplate, clock);
    }

    @Bean
    InboxRepository inboxRepository(
            MongoTemplate mongoTemplate,
            @Value("${app.inbox.consumer-name:notification-service}") String consumerName,
            Clock clock) {
        return new MongoInboxRepository(mongoTemplate, consumerName, clock);
    }

    @Bean
    NotificacionPort notificacionPort(
            WhatsAppProperties properties,
            CircuitBreakerRegistry circuitBreakers,
            RetryRegistry retries,
            MeterRegistry meterRegistry) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(properties.timeoutSeconds());
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        RestClient restClient =
                RestClient.builder()
                        .baseUrl(properties.baseUrl())
                        .requestFactory(requestFactory)
                        .build();
        return new WhatsAppNotificationAdapter(
                new WhatsAppClient(restClient, properties.token()),
                circuitBreakers.circuitBreaker(WHATSAPP),
                retries.retry(WHATSAPP),
                meterRegistry);
    }

    @Bean
    NotificarPolizaEmitidaUseCase notificarPolizaEmitida(
            ContactoClienteRepository contactos,
            InboxRepository inbox,
            NotificacionPort notificaciones) {
        return new NotificarPolizaEmitidaUseCase(contactos, inbox, notificaciones);
    }

    @Bean
    ActualizarContactoClienteUseCase actualizarContactoCliente(
            ContactoClienteRepository contactos, InboxRepository inbox) {
        return new ActualizarContactoClienteUseCase(contactos, inbox);
    }

    @Bean
    PolicyIssuedListener policyIssuedListener(NotificarPolizaEmitidaUseCase useCase) {
        return new PolicyIssuedListener(useCase);
    }

    @Bean
    CustomerEventsListener customerEventsListener(ActualizarContactoClienteUseCase useCase) {
        return new CustomerEventsListener(useCase);
    }

    @Bean
    PausaListenerPorCircuitoAbierto pausaListenerPorCircuitoAbierto(
            CircuitBreakerRegistry circuitBreakers,
            RabbitListenerEndpointRegistry listeners,
            MeterRegistry meterRegistry) {
        return new PausaListenerPorCircuitoAbierto(
                circuitBreakers.circuitBreaker(WHATSAPP),
                () -> listeners.getListenerContainer(PolicyIssuedListener.LISTENER_ID),
                Executors.newSingleThreadExecutor(
                        runnable -> {
                            Thread thread = new Thread(runnable, "pausa-listener-whatsapp");
                            thread.setDaemon(true);
                            return thread;
                        }),
                meterRegistry);
    }

    @Bean
    MonitorDeadLetterQueues monitorDeadLetterQueues(
            AmqpAdmin admin, RabbitMqProperties properties, MeterRegistry meterRegistry) {
        return new MonitorDeadLetterQueues(
                admin,
                List.of(
                        properties.policy().deadLetterQueue(),
                        properties.customer().deadLetterQueue()),
                meterRegistry);
    }
}
