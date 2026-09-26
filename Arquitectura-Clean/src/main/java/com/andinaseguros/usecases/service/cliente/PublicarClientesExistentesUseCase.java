package com.andinaseguros.usecases.service.cliente;

import com.andinaseguros.entities.model.Cliente;
import com.andinaseguros.usecases.mapper.ClienteEventMapper;
import com.andinaseguros.usecases.port.out.event.DomainEventPublisherPort;
import com.andinaseguros.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.usecases.port.out.repository.ClienteRepository;
import com.andinaseguros.usecases.port.out.time.ClockPort;

/**
 * Backfill: publica un customer.registered.v1 por cada cliente existente, con su versión actual.
 * Sirve para poblar una proyección nueva (o reconstruirla). Los consumidores descartan lo que ya
 * tienen, así que ejecutarlo varias veces no causa daño.
 */
public class PublicarClientesExistentesUseCase {

    private final ClienteRepository clienteRepository;
    private final DomainEventPublisherPort eventPublisher;
    private final ClockPort clock;
    private final IdGeneratorPort ids;

    public PublicarClientesExistentesUseCase(
            ClienteRepository clienteRepository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            IdGeneratorPort ids) {
        this.clienteRepository = clienteRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.ids = ids;
    }

    public int execute() {
        int publicados = 0;
        for (Cliente cliente : clienteRepository.listar()) {
            eventPublisher.publicar(
                    ClienteEventMapper.registrado(ids.generar(), clock.now(), cliente));
            publicados++;
        }
        return publicados;
    }
}
