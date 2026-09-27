package com.andinaseguros.policy.usecases.port.out.event;

import com.andinaseguros.policy.entities.event.DomainEvent;

public interface DomainEventPublisherPort {
    void publicar(DomainEvent event);
}
