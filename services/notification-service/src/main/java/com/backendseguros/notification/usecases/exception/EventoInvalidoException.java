package com.backendseguros.notification.usecases.exception;

/** El evento no cumple su contrato (tipo, versión o campos obligatorios). Va a la DLQ. */
public class EventoInvalidoException extends RuntimeException {
    public EventoInvalidoException(String message) {
        super(message);
    }
}
