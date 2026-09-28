package com.backendseguros.notification.usecases.service;

import com.backendseguros.notification.entities.model.ContactoCliente;
import com.backendseguros.notification.usecases.dto.ContactoClienteCommand;
import com.backendseguros.notification.usecases.exception.EventoInvalidoException;
import com.backendseguros.notification.usecases.port.out.ContactoClienteRepository;
import com.backendseguros.notification.usecases.port.out.InboxRepository;

/**
 * Mantiene la proyección customer_contacts con los eventos customer.registered.v1 y
 * customer.updated.v1. Tolera eventos repetidos (inbox) y desordenados (versión del agregado).
 */
public class ActualizarContactoClienteUseCase {

    public enum Resultado {
        APLICADO,
        VERSION_ANTIGUA,
        DUPLICADO
    }

    private final ContactoClienteRepository contactos;
    private final InboxRepository inbox;

    public ActualizarContactoClienteUseCase(
            ContactoClienteRepository contactos, InboxRepository inbox) {
        this.contactos = contactos;
        this.inbox = inbox;
    }

    public Resultado execute(ContactoClienteCommand command) {
        if (command.eventId() == null || command.clienteId() == null || command.version() < 1) {
            throw new EventoInvalidoException(
                    command.tipoEvento() + " sin eventId, customerId o aggregateVersion");
        }
        if (inbox.fueProcesado(command.eventId())) {
            return Resultado.DUPLICADO;
        }

        boolean aplicado =
                contactos.guardarSiEsMasNuevo(
                        new ContactoCliente(
                                command.clienteId(),
                                command.nombre(),
                                command.correo(),
                                command.telefono(),
                                command.version()));
        inbox.registrar(command.eventId(), command.tipoEvento());
        return aplicado ? Resultado.APLICADO : Resultado.VERSION_ANTIGUA;
    }
}
