package com.backendseguros.notification.interfaceadapters.out.persistence.mongodb;

import com.backendseguros.notification.usecases.port.out.InboxRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.mongodb.core.MongoTemplate;

public class MongoInboxRepository implements InboxRepository {
    private final MongoTemplate mongo;
    private final String consumer;
    private final Clock clock;

    public MongoInboxRepository(MongoTemplate mongo, String consumer, Clock clock) {
        this.mongo = mongo;
        this.consumer = consumer;
        this.clock = clock;
    }

    @Override
    public boolean fueProcesado(UUID eventId) {
        return mongo.findById(key(eventId), InboxDocument.class) != null;
    }

    @Override
    public void registrar(UUID eventId, String tipoEvento) {
        InboxDocument document = new InboxDocument();
        document.id = key(eventId);
        document.eventType = tipoEvento;
        document.consumer = consumer;
        document.processedAt = clock.instant();
        mongo.save(document);
    }

    private String key(UUID eventId) {
        return eventId + ":" + consumer;
    }
}
