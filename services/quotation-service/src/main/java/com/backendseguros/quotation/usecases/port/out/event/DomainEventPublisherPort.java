package com.backendseguros.quotation.usecases.port.out.event;

import com.backendseguros.quotation.entities.event.DomainEvent;

public interface DomainEventPublisherPort {
    void publicar(DomainEvent event);
}
