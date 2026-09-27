package com.andinaseguros.customer.usecases.port.out.event;

import com.andinaseguros.customer.entities.event.DomainEvent;

public interface DomainEventPublisherPort {
    void publicar(DomainEvent event);
}
