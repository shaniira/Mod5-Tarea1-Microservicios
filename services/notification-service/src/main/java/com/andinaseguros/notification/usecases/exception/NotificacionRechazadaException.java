package com.andinaseguros.notification.usecases.exception;

/** El proveedor rechazó el mensaje (número inválido, token incorrecto...). Va a la DLQ. */
public class NotificacionRechazadaException extends RuntimeException {
    public NotificacionRechazadaException(String message) {
        super(message);
    }

    public NotificacionRechazadaException(String message, Throwable cause) {
        super(message, cause);
    }
}
