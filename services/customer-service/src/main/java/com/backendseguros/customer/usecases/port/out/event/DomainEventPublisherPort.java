package com.backendseguros.customer.usecases.port.out.event;

import com.backendseguros.customer.entities.event.DomainEvent;

public interface DomainEventPublisherPort {
    void publicar(DomainEvent event);
}
