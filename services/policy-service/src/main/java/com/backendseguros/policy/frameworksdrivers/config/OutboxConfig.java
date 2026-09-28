package com.backendseguros.policy.frameworksdrivers.config;

import com.backendseguros.policy.interfaceadapters.out.event.IntegrationEventMapper;
import com.backendseguros.policy.interfaceadapters.out.event.MongoOutboxLease;
import com.backendseguros.policy.interfaceadapters.out.event.OutboxDomainEventPublisherAdapter;
import com.backendseguros.policy.interfaceadapters.out.event.OutboxRelay;
import com.backendseguros.policy.interfaceadapters.out.event.RabbitMqProperties;
import com.backendseguros.policy.interfaceadapters.out.persistence.mongodb.adapter.MongoTransaccionAdapter;
import com.backendseguros.policy.interfaceadapters.out.persistence.mongodb.repository.SpringDataOutboxMongoRepository;
import com.backendseguros.policy.usecases.port.out.event.DomainEventPublisherPort;
import com.backendseguros.policy.usecases.port.out.transaccion.TransaccionPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Outbox transaccional (sección 5.2 de la propuesta): publicar eventos sin perderlos. */
@Configuration
@EnableScheduling
public class OutboxConfig {

    @Bean
    MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }

    @Bean
    TransaccionPort transaccionPort(
            MongoTemplate mongoTemplate, MongoTransactionManager mongoTransactionManager) {
        return new MongoTransaccionAdapter(mongoTemplate, mongoTransactionManager);
    }

    @Bean
    IntegrationEventMapper integrationEventMapper(RabbitMqProperties properties) {
        return new IntegrationEventMapper(properties);
    }

    @Bean
    DomainEventPublisherPort domainEventPublisher(
            SpringDataOutboxMongoRepository outbox,
            IntegrationEventMapper mapper,
            ObjectMapper objectMapper,
            ObjectProvider<Tracer> tracer) {
        return new OutboxDomainEventPublisherAdapter(
                outbox, mapper, objectMapper, () -> traceparent(tracer.getIfAvailable()));
    }

    /** traceparent W3C de la traza en curso (null si no hay traza activa). */
    private static String traceparent(Tracer tracer) {
        Span span = tracer == null ? null : tracer.currentSpan();
        if (span == null) {
            return null;
        }
        TraceContext contexto = span.context();
        return "00-" + contexto.traceId() + "-" + contexto.spanId() + "-01";
    }

    @Bean
    OutboxRelay outboxRelay(
            SpringDataOutboxMongoRepository outbox,
            RabbitTemplate rabbitTemplate,
            @Value("${app.outbox.batch-size:100}") int batchSize,
            @Value("${app.outbox.confirm-timeout-ms:5000}") long confirmTimeoutMs,
            @Value("${app.outbox.lease-seconds:15}") long leaseSeconds,
            MongoTemplate mongoTemplate,
            MeterRegistry meterRegistry) {
        // El relay renueva el turno antes de cada publicación y una publicación espera como mucho
        // confirmTimeout: si el turno durara menos que eso (con margen), podría vencerse mientras
        // se publica y otra réplica publicaría a la vez.
        if (Duration.ofSeconds(leaseSeconds).compareTo(Duration.ofMillis(confirmTimeoutMs).multipliedBy(2)) <= 0) {
            throw new IllegalStateException(
                    "app.outbox.lease-seconds (" + leaseSeconds + " s) debe ser mayor que el doble de "
                            + "app.outbox.confirm-timeout-ms (" + confirmTimeoutMs + " ms)");
        }
        OutboxRelay relay =
                new OutboxRelay(
                        outbox,
                        rabbitTemplate,
                        batchSize,
                        Duration.ofMillis(confirmTimeoutMs),
                        Clock.systemUTC(),
                        // Fase 7: con varias réplicas publica solo la que tiene el turno.
                        new MongoOutboxLease(
                                mongoTemplate,
                                instancia(),
                                Duration.ofSeconds(leaseSeconds),
                                Clock.systemUTC()));
        // Alertas propuestas: eventos pendientes de más de 5 minutos (sección 7.2).
        Gauge.builder("outbox.events.pending", relay, OutboxRelay::pendientes)
                .description("Eventos del Outbox aun no publicados en RabbitMQ")
                .register(meterRegistry);
        Gauge.builder(
                        "outbox.events.oldest.pending.age",
                        relay,
                        OutboxRelay::antiguedadPendienteMasViejoSegundos)
                .description("Antiguedad del evento pendiente mas viejo del Outbox")
                .baseUnit("seconds")
                .register(meterRegistry);
        return relay;
    }

    /**
     * Interruptor de publicación (pasos 6.6 y 6.7: "cambiar la fuente de los eventos a
     * policy-service, interruptor único"). Mientras el monolito emita las pólizas, él es la fuente
     * de policy.issued; policy-service corre en paralelo y sus eventos se quedan en el Outbox
     * (PENDING). Se activa (POLICY_EVENTS_PUBLISH_ENABLED=true) en el mismo momento que se cambian
     * las rutas.
     */
    @Bean
    @ConditionalOnProperty(name = "app.outbox.publish-enabled", havingValue = "true", matchIfMissing = true)
    OutboxRelayScheduler outboxRelayScheduler(OutboxRelay relay) {
        return new OutboxRelayScheduler(relay);
    }

    /** Nombre del contenedor o Pod (HOSTNAME) y un sufijo, para distinguir réplicas en los logs. */
    private static String instancia() {
        String host = System.getenv().getOrDefault("HOSTNAME", "local");
        return host + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    static class OutboxRelayScheduler {
        private final OutboxRelay relay;

        OutboxRelayScheduler(OutboxRelay relay) {
            this.relay = relay;
        }

        @Scheduled(
                fixedDelayString = "${app.outbox.relay-interval-ms:1000}",
                initialDelayString = "${app.outbox.relay-initial-delay-ms:5000}")
        void publicarPendientes() {
            relay.publicarPendientes();
        }
    }
}
