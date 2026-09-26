package com.andinaseguros.usecases.port.out.event;

import com.andinaseguros.entities.event.DomainEvent;

public interface DomainEventPublisherPort {
    void publicar(DomainEvent event);
}
