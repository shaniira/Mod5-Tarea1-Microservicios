package com.andinaseguros.customer.usecases.service.cliente;

import static com.andinaseguros.customer.usecases.mapper.ClienteResponseMapper.toResponse;

import com.andinaseguros.customer.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.customer.entities.exception.ReglaNegocioException;
import com.andinaseguros.customer.entities.model.Cliente;
import com.andinaseguros.customer.usecases.dto.ActualizarContactoClienteRequestModel;
import com.andinaseguros.customer.usecases.dto.Responses.ClienteResponse;
import com.andinaseguros.customer.usecases.mapper.ClienteEventMapper;
import com.andinaseguros.customer.usecases.port.out.event.DomainEventPublisherPort;
import com.andinaseguros.customer.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.customer.usecases.port.out.repository.ClienteRepository;
import com.andinaseguros.customer.usecases.port.out.time.ClockPort;
import com.andinaseguros.customer.usecases.port.out.transaccion.TransaccionPort;

/**
 * Cambia el correo y el teléfono de un cliente y publica customer.updated.v1, que es lo que
 * mantiene al día la proyección de contactos de notification-service.
 */
public class ActualizarContactoClienteUseCase {

    private final ClienteRepository clienteRepository;
    private final DomainEventPublisherPort eventPublisher;
    private final TransaccionPort transaccion;
    private final ClockPort clock;
    private final IdGeneratorPort ids;

    public ActualizarContactoClienteUseCase(
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

    public ClienteResponse execute(ActualizarContactoClienteRequestModel solicitud) {
        Cliente actual =
                clienteRepository
                        .buscarPorId(solicitud.clienteId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Cliente"));

        if (actual.tieneMismoContacto(solicitud.correo(), solicitud.telefono())) {
            return toResponse(actual);
        }
        validarCorreoDisponible(actual, solicitud.correo());

        Cliente actualizado = actual.conContacto(solicitud.correo(), solicitud.telefono());
        Cliente guardado =
                transaccion.ejecutar(
                        () -> {
                            Cliente resultado = clienteRepository.guardar(actualizado);
                            eventPublisher.publicar(
                                    ClienteEventMapper.actualizado(
                                            ids.generar(), clock.now(), resultado));
                            return resultado;
                        });
        return toResponse(guardado);
    }

    private void validarCorreoDisponible(Cliente actual, String correo) {
        if (correo == null || correo.equalsIgnoreCase(actual.getCorreo())) {
            return;
        }
        clienteRepository
                .buscarPorCorreo(correo)
                .filter(otro -> !otro.getId().equals(actual.getId()))
                .ifPresent(
                        otro -> {
                            throw new ReglaNegocioException(
                                    "CORREO_DUPLICADO", "Otro cliente ya usa ese correo");
                        });
    }
}
