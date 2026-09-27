package com.andinaseguros.customer.interfaceadapters.out.persistence.mongodb.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Evento pendiente de publicar (patrón Outbox). Se inserta en la misma transacción que el cambio
 * de negocio; el relay lo publica en RabbitMQ y lo marca como enviado.
 */
@Document("outbox")
@CompoundIndex(name = "status_createdAt", def = "{'status': 1, 'createdAt': 1}")
public class OutboxEventDocument {
    public static final String PENDING = "PENDING";
    public static final String SENT = "SENT";

    @Id public String id;
    public String exchange;
    public String routingKey;
    public String eventType;
    public String aggregateId;
    public String payload;
    public String correlationId;
    // Contexto W3C de la traza que originó el evento: el consumidor continúa la misma traza.
    public String traceparent;
    public String status;
    public int attempts;
    public Instant createdAt;

    // Los eventos enviados se borran solos a los 7 días; los pendientes no tienen sentAt.
    @Indexed(expireAfterSeconds = 604800)
    public Instant sentAt;

    public String lastError;
}
