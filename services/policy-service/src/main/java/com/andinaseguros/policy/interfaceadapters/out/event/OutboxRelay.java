package com.andinaseguros.policy.interfaceadapters.out.event;

import com.andinaseguros.policy.interfaceadapters.out.persistence.mongodb.document.OutboxEventDocument;
import com.andinaseguros.policy.interfaceadapters.out.persistence.mongodb.repository.SpringDataOutboxMongoRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;

/**
 * Publica en RabbitMQ los eventos pendientes del Outbox, en orden de creación, y solo los marca
 * como enviados cuando el broker confirma la recepción (publisher confirms). Si RabbitMQ está caído
 * o no confirma, el evento se queda pendiente y se reintenta en la siguiente pasada; nada se
 * pierde. Se detiene en el primer fallo para no adelantar eventos posteriores del mismo agregado.
 *
 * <p>Con varias réplicas solo publica la que tiene el turno ({@link MongoOutboxLease}), y lo renueva
 * antes de cada evento, no solo al empezar la pasada: como una publicación espera como mucho
 * {@code confirmTimeout} y el turno dura más del doble (se valida al arrancar), nunca se publica
 * con el turno vencido aunque el lote sea largo. Si otra réplica lo tomó, la pasada se corta y
 * los eventos que faltan los publica la nueva dueña, en orden. Riesgo residual: una pausa de la
 * JVM más larga que el turno justo entre renovar y publicar; entonces un evento puede salir dos
 * veces, y los consumidores lo descartan (inbox o versión).
 */
public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final SpringDataOutboxMongoRepository outbox;
    private final RabbitTemplate rabbitTemplate;
    private final int batchSize;
    private final Duration confirmTimeout;
    private final Clock clock;
    private final BooleanSupplier turno;

    public OutboxRelay(
            SpringDataOutboxMongoRepository outbox,
            RabbitTemplate rabbitTemplate,
            int batchSize,
            Duration confirmTimeout,
            Clock clock) {
        this(outbox, rabbitTemplate, batchSize, confirmTimeout, clock, () -> true);
    }

    public OutboxRelay(
            SpringDataOutboxMongoRepository outbox,
            RabbitTemplate rabbitTemplate,
            int batchSize,
            Duration confirmTimeout,
            Clock clock,
            BooleanSupplier turno) {
        this.outbox = outbox;
        this.rabbitTemplate = rabbitTemplate;
        this.batchSize = batchSize;
        this.confirmTimeout = confirmTimeout;
        this.clock = clock;
        this.turno = turno;
    }

    /** Devuelve cuántos eventos se enviaron en esta pasada. */
    public int publicarPendientes() {
        if (!turno.getAsBoolean()) {
            return 0;
        }
        List<OutboxEventDocument> pendientes =
                outbox.findByStatusOrderByCreatedAtAsc(
                        OutboxEventDocument.PENDING, PageRequest.of(0, batchSize));
        int enviados = 0;
        for (OutboxEventDocument evento : pendientes) {
            // Renovar antes de cada evento; el primero ya quedó cubierto al empezar la pasada.
            if (enviados > 0 && !turno.getAsBoolean()) {
                log.info("Relay del Outbox: se perdió el turno a mitad de la pasada tras {} evento(s)", enviados);
                break;
            }
            try {
                enviar(evento);
            } catch (Exception exception) {
                evento.attempts++;
                evento.lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
                outbox.save(evento);
                // Con RabbitMQ caído esto se repite cada segundo: se avisa en el primer intento y
                // luego cada 60; la métrica outbox_events_oldest_pending_age muestra el retraso.
                if (evento.attempts == 1 || evento.attempts % 60 == 0) {
                    log.warn(
                            "No se pudo publicar el evento {} ({}), intento {}. Queda pendiente: {}",
                            evento.id,
                            evento.eventType,
                            evento.attempts,
                            evento.lastError);
                }
                break;
            }
            evento.status = OutboxEventDocument.SENT;
            evento.sentAt = clock.instant();
            evento.lastError = null;
            outbox.save(evento);
            enviados++;
            log.debug("Evento {} ({}) publicado en {}", evento.id, evento.eventType, evento.routingKey);
        }
        return enviados;
    }

    public long pendientes() {
        return outbox.countByStatus(OutboxEventDocument.PENDING);
    }

    /** Antigüedad en segundos del evento pendiente más viejo (0 si no hay pendientes). */
    public long antiguedadPendienteMasViejoSegundos() {
        return outbox.findFirstByStatusOrderByCreatedAtAsc(OutboxEventDocument.PENDING)
                .map(e -> Math.max(0, Duration.between(e.createdAt, clock.instant()).toSeconds()))
                .orElse(0L);
    }

    private void enviar(OutboxEventDocument evento) throws Exception {
        CorrelationData correlation = new CorrelationData(evento.id);
        rabbitTemplate.send(evento.exchange, evento.routingKey, toAmqp(evento), correlation);

        CorrelationData.Confirm confirm =
                correlation.getFuture().get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!confirm.isAck()) {
            throw new IllegalStateException("RabbitMQ rechazo el mensaje: " + confirm.getReason());
        }
        if (correlation.getReturned() != null) {
            // El exchange existe pero ninguna cola escucha esa routing key todavía. No se reintenta
            // (no cambiaría nada): el consumidor que llegue tarde se puebla con el backfill.
            log.warn(
                    "Evento {} ({}) sin cola enlazada a {} en {}",
                    evento.id,
                    evento.eventType,
                    evento.routingKey,
                    evento.exchange);
        }
    }

    private Message toAmqp(OutboxEventDocument evento) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setMessageId(evento.id);
        properties.setType(evento.eventType);
        properties.setTimestamp(Date.from(evento.createdAt));
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        if (evento.correlationId != null) {
            properties.setHeader(CORRELATION_ID_HEADER, evento.correlationId);
        }
        if (evento.traceparent != null) {
            // Propagación W3C: los consumidores (notification, identity, backend) continúan esta traza.
            properties.setHeader("traceparent", evento.traceparent);
        }
        return new Message(evento.payload.getBytes(StandardCharsets.UTF_8), properties);
    }
}
