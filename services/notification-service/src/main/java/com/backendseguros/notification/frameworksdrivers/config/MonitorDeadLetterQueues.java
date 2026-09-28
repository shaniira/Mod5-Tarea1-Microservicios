package com.backendseguros.notification.frameworksdrivers.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * La DLQ deja de ser un cementerio (sección 6.3 de la propuesta, riesgo A6): expone cuántos
 * mensajes hay en cada DLQ como métrica (notification_dlq_messages) para alertar, y deja un WARN en
 * el log cada vez que el número cambia y no es cero. El reproceso está en el README del servicio.
 */
public class MonitorDeadLetterQueues {
    private static final Logger log = LoggerFactory.getLogger(MonitorDeadLetterQueues.class);

    private final AmqpAdmin admin;
    private final List<String> colas;
    private final Map<String, AtomicLong> mensajes = new ConcurrentHashMap<>();

    public MonitorDeadLetterQueues(AmqpAdmin admin, List<String> colas, MeterRegistry meterRegistry) {
        this.admin = admin;
        this.colas = colas;
        for (String cola : colas) {
            AtomicLong valor = new AtomicLong(0);
            mensajes.put(cola, valor);
            Gauge.builder("notification.dlq.messages", valor, AtomicLong::get)
                    .description("Mensajes en la DLQ (deberia ser 0)")
                    .tag("queue", cola)
                    .register(meterRegistry);
        }
    }

    @Scheduled(
            fixedDelayString = "${app.dlq-monitor.interval-ms:30000}",
            initialDelayString = "${app.dlq-monitor.initial-delay-ms:10000}")
    public void revisar() {
        for (String cola : colas) {
            try {
                Properties propiedades = admin.getQueueProperties(cola);
                if (propiedades == null) {
                    continue;
                }
                long actual = ((Number) propiedades.get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).longValue();
                long anterior = mensajes.get(cola).getAndSet(actual);
                if (actual > 0 && actual != anterior) {
                    log.warn(
                            "La DLQ {} tiene {} mensaje(s). Revisar la causa y reprocesar (README).",
                            cola,
                            actual);
                }
            } catch (RuntimeException e) {
                log.debug("No se pudo leer el tamano de la DLQ {}: {}", cola, e.getMessage());
            }
        }
    }
}
