package com.andinaseguros.notification.idempotency;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("processed_notification_events")
public class ProcessedEvent {
    @Id public String id;
    public String eventId;
    public String consumer;
    public Instant processedAt;

    public ProcessedEvent() {}

    public ProcessedEvent(String eventId, String consumer, Instant processedAt) {
        this.id = eventId + ":" + consumer;
        this.eventId = eventId;
        this.consumer = consumer;
        this.processedAt = processedAt;
    }
}
