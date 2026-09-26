package com.andinaseguros.notification.idempotency;

import com.andinaseguros.notification.config.IdempotencyProperties;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {
    private final ProcessedEventRepository repository;
    private final IdempotencyProperties properties;

    public IdempotencyService(ProcessedEventRepository repository, IdempotencyProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public boolean wasProcessed(UUID eventId) {
        return repository.existsById(key(eventId));
    }

    public void markProcessed(UUID eventId) {
        repository.save(new ProcessedEvent(eventId.toString(), properties.consumerName(), Instant.now()));
    }

    private String key(UUID eventId) {
        return eventId + ":" + properties.consumerName();
    }
}
