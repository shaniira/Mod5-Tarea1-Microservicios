package com.andinaseguros.notification.usecases.service;

import com.andinaseguros.notification.entities.model.ContactoCliente;
import com.andinaseguros.notification.entities.model.Notificacion;
import com.andinaseguros.notification.usecases.dto.PolizaEmitidaCommand;
import com.andinaseguros.notification.usecases.exception.ContactoNoDisponibleException;
import com.andinaseguros.notification.usecases.exception.EventoInvalidoException;
import com.andinaseguros.notification.usecases.port.out.ContactoClienteRepository;
import com.andinaseguros.notification.usecases.port.out.InboxRepository;
import com.andinaseguros.notification.usecases.port.out.NotificacionPort;

/**
 * Envía el WhatsApp de póliza emitida. El teléfono sale de la proyección local customer_contacts,
 * no de la base del backend.
 */
public class NotificarPolizaEmitidaUseCase {
    public static final String TIPO_EVENTO = "PolicyIssued";

    public enum Resultado {
        ENVIADA,
        DUPLICADO
    }

    private final ContactoClienteRepository contactos;
    private final InboxRepository inbox;
    private final NotificacionPort notificaciones;

    public NotificarPolizaEmitidaUseCase(
            ContactoClienteRepository contactos,
            InboxRepository inbox,
            NotificacionPort notificaciones) {
        this.contactos = contactos;
        this.inbox = inbox;
        this.notificaciones = notificaciones;
    }

    public Resultado execute(PolizaEmitidaCommand command) {
        validar(command);
        if (inbox.fueProcesado(command.eventId())) {
            return Resultado.DUPLICADO;
        }

        ContactoCliente contacto =
                contactos
                        .buscar(command.clienteId())
                        .orElseThrow(
                                () ->
                                        new ContactoNoDisponibleException(
                                                "Cliente sin contacto en la proyeccion: "
                                                        + command.clienteId()));
        if (!contacto.tieneTelefono()) {
            throw new ContactoNoDisponibleException(
                    "El cliente no tiene telefono: " + command.clienteId());
        }

        notificaciones.enviar(Notificacion.polizaEmitida(contacto, command.numeroPoliza()));
        // El WhatsApp no se puede deshacer: se marca después de enviarlo (al menos una vez).
        inbox.registrar(command.eventId(), TIPO_EVENTO);
        return Resultado.ENVIADA;
    }

    private void validar(PolizaEmitidaCommand command) {
        if (command.eventId() == null
                || command.clienteId() == null
                || command.numeroPoliza() == null
                || command.numeroPoliza().isBlank()) {
            throw new EventoInvalidoException("PolicyIssued sin eventId, customerId o policyNumber");
        }
    }
}
