package com.backendseguros.notification.interfaceadapters.out.persistence.mongodb;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Reemplaza a processed_notification_events, que vivía en la base del backend. */
@Document("inbox")
public class InboxDocument {
    @Id public String id;
    public String eventType;
    public String consumer;

    // Se conserva 30 días: más que cualquier reintento o reproceso razonable de la DLQ.
    @Indexed(expireAfterSeconds = 2592000)
    public Instant processedAt;
}
