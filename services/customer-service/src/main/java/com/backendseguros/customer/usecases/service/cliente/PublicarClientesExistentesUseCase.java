package com.backendseguros.customer.usecases.service.cliente;

import com.backendseguros.customer.entities.model.Cliente;
import com.backendseguros.customer.entities.model.Vehiculo;
import com.backendseguros.customer.usecases.dto.Responses.ReenvioEventosResponse;
import com.backendseguros.customer.usecases.mapper.ClienteEventMapper;
import com.backendseguros.customer.usecases.mapper.VehiculoEventMapper;
import com.backendseguros.customer.usecases.port.out.event.DomainEventPublisherPort;
import com.backendseguros.customer.usecases.port.out.id.IdGeneratorPort;
import com.backendseguros.customer.usecases.port.out.repository.ClienteRepository;
import com.backendseguros.customer.usecases.port.out.repository.VehiculoRepository;
import com.backendseguros.customer.usecases.port.out.time.ClockPort;

/**
 * Backfill (paso 3.4): publica un customer.registered.v1 por cada cliente, con su versión actual, y
 * un vehicle.registered.v1 por cada vehículo. Puebla o reconstruye las proyecciones de
 * notification, identity y el backend. Los consumidores descartan lo que ya tienen, así que
 * ejecutarlo varias veces no causa daño. Primero van todos los clientes y después los vehículos.
 */
public class PublicarClientesExistentesUseCase {

    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final DomainEventPublisherPort eventPublisher;
    private final ClockPort clock;
    private final IdGeneratorPort ids;

    public PublicarClientesExistentesUseCase(
            ClienteRepository clienteRepository,
            VehiculoRepository vehiculoRepository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            IdGeneratorPort ids) {
        this.clienteRepository = clienteRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.ids = ids;
    }

    public ReenvioEventosResponse execute() {
        int clientes = 0;
        for (Cliente cliente : clienteRepository.listar()) {
            eventPublisher.publicar(
                    ClienteEventMapper.registrado(ids.generar(), clock.now(), cliente));
            clientes++;
        }
        int vehiculos = 0;
        for (Vehiculo vehiculo : vehiculoRepository.listar()) {
            eventPublisher.publicar(
                    VehiculoEventMapper.registrado(ids.generar(), clock.now(), vehiculo));
            vehiculos++;
        }
        return new ReenvioEventosResponse(clientes, vehiculos);
    }
}
