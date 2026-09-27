package com.andinaseguros.quotation.usecases.port.out.event;

import com.andinaseguros.quotation.entities.event.DomainEvent;

public interface DomainEventPublisherPort {
    void publicar(DomainEvent event);
}
