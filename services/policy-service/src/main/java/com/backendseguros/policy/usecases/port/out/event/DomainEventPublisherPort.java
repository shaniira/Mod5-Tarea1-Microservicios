package com.backendseguros.policy.usecases.port.out.event;

import com.backendseguros.policy.entities.event.DomainEvent;

public interface DomainEventPublisherPort {
    void publicar(DomainEvent event);
}
