package com.andinaseguros.usecases.service.cliente;

import static com.andinaseguros.usecases.mapper.ClienteResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.CrearClienteRequestModel;
import com.andinaseguros.usecases.dto.Responses.ClienteResponse;
import com.andinaseguros.usecases.mapper.ClienteEventMapper;
import com.andinaseguros.usecases.port.in.RegistrarClienteUseCase;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.model.Cliente;
import com.andinaseguros.usecases.port.out.event.DomainEventPublisherPort;
import com.andinaseguros.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.usecases.port.out.repository.ClienteRepository;
import com.andinaseguros.usecases.port.out.time.ClockPort;
import com.andinaseguros.usecases.port.out.transaccion.TransaccionPort;

public class CrearClienteUseCase implements RegistrarClienteUseCase {

    private final ClienteRepository clienteRepository;
    private final DomainEventPublisherPort eventPublisher;
    private final TransaccionPort transaccion;
    private final ClockPort clock;
    private final IdGeneratorPort ids;

    public CrearClienteUseCase(
            ClienteRepository clienteRepository,
            DomainEventPublisherPort eventPublisher,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        this.clienteRepository = clienteRepository;
        this.eventPublisher = eventPublisher;
        this.transaccion = transaccion;
        this.clock = clock;
        this.ids = ids;
    }

    @Override
    public ClienteResponse execute(CrearClienteRequestModel solicitud) {
        boolean documentoRegistrado =
                clienteRepository.buscarPorDocumento(solicitud.numeroDocumento()).isPresent();

        if (documentoRegistrado) {
            throw new ReglaNegocioException(
                    "DOCUMENTO_DUPLICADO", "Ya existe un cliente con ese documento");
        }

        Cliente cliente =
                new Cliente(
                        ids.generar(),
                        solicitud.tipoDocumento(),
                        solicitud.numeroDocumento(),
                        solicitud.nombres(),
                        solicitud.apellidos(),
                        solicitud.fechaNacimiento(),
                        solicitud.correo(),
                        solicitud.telefono(),
                        true);

        // El cliente y su evento customer.registered.v1 (en el Outbox) se guardan juntos.
        Cliente clienteGuardado =
                transaccion.ejecutar(
                        () -> {
                            Cliente guardado = clienteRepository.guardar(cliente);
                            eventPublisher.publicar(
                                    ClienteEventMapper.registrado(
                                            ids.generar(), clock.now(), guardado));
                            return guardado;
                        });

        return toResponse(clienteGuardado);
    }
}
