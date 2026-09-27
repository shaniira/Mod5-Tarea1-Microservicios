package com.andinaseguros.claims.usecases.port.out.event;

import com.andinaseguros.claims.entities.event.DomainEvent;

public interface DomainEventPublisherPort {
    void publicar(DomainEvent event);
}
