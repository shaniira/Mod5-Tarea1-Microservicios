package com.backendseguros.claims.usecases.port.out.event;

import com.backendseguros.claims.entities.event.DomainEvent;

public interface DomainEventPublisherPort {
    void publicar(DomainEvent event);
}
