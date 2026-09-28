package com.backendseguros.customer.usecases.service.vehiculo;

import static com.backendseguros.customer.usecases.mapper.VehiculoResponseMapper.toResponse;

import com.backendseguros.customer.entities.exception.RecursoNoEncontradoException;
import com.backendseguros.customer.entities.exception.ReglaNegocioException;
import com.backendseguros.customer.entities.model.Vehiculo;
import com.backendseguros.customer.entities.valueobject.Placa;
import com.backendseguros.customer.usecases.dto.CrearVehiculoRequestModel;
import com.backendseguros.customer.usecases.dto.Responses.VehiculoResponse;
import com.backendseguros.customer.usecases.mapper.VehiculoEventMapper;
import com.backendseguros.customer.usecases.port.in.RegistrarVehiculoUseCase;
import com.backendseguros.customer.usecases.port.out.event.DomainEventPublisherPort;
import com.backendseguros.customer.usecases.port.out.id.IdGeneratorPort;
import com.backendseguros.customer.usecases.port.out.repository.ClienteRepository;
import com.backendseguros.customer.usecases.port.out.repository.VehiculoRepository;
import com.backendseguros.customer.usecases.port.out.time.ClockPort;
import com.backendseguros.customer.usecases.port.out.transaccion.TransaccionPort;
import java.util.UUID;

/**
 * Registra un vehículo y publica vehicle.registered.v1 en la misma transacción (Outbox). Los datos
 * llegan del formulario: si JSON.pe no respondió, el usuario los ingresa a mano (paso 3.2), así que
 * registrar un vehículo nunca depende del proveedor externo.
 */
public class CrearVehiculoUseCase implements RegistrarVehiculoUseCase {

    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final DomainEventPublisherPort eventPublisher;
    private final TransaccionPort transaccion;
    private final ClockPort clock;
    private final IdGeneratorPort ids;

    public CrearVehiculoUseCase(
            ClienteRepository clienteRepository,
            VehiculoRepository vehiculoRepository,
            DomainEventPublisherPort eventPublisher,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        this.clienteRepository = clienteRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.eventPublisher = eventPublisher;
        this.transaccion = transaccion;
        this.clock = clock;
        this.ids = ids;
    }

    @Override
    public VehiculoResponse execute(CrearVehiculoRequestModel solicitud) {
        validarCliente(solicitud.clienteId());
        validarPlacaDisponible(solicitud.placa());
        Vehiculo vehiculo =
                new Vehiculo(
                        ids.generar(),
                        solicitud.clienteId(),
                        new Placa(solicitud.placa()),
                        solicitud.marca(),
                        solicitud.modelo(),
                        solicitud.anioFabricacion(),
                        solicitud.tipo(),
                        solicitud.uso(),
                        solicitud.zonaCirculacion());

        Vehiculo vehiculoGuardado =
                transaccion.ejecutar(
                        () -> {
                            Vehiculo guardado = vehiculoRepository.guardar(vehiculo);
                            eventPublisher.publicar(
                                    VehiculoEventMapper.registrado(
                                            ids.generar(), clock.now(), guardado));
                            return guardado;
                        });

        return toResponse(vehiculoGuardado);
    }

    private void validarCliente(UUID clienteId) {
        clienteRepository
                .buscarPorId(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente"));
    }

    private void validarPlacaDisponible(String placa) {
        if (vehiculoRepository.buscarPorPlaca(new Placa(placa).valor()).isPresent()) {
            throw new ReglaNegocioException("PLACA_DUPLICADA", "La placa ya está registrada");
        }
    }
}
