package com.andinaseguros.notification.usecases.exception;

/**
 * El canal de envío (WhatsApp) está caído o su circuit breaker está abierto. Es transitorio: el
 * mensaje vuelve a la cola y el listener se pausa hasta que el canal se recupere.
 */
public class CanalNoDisponibleException extends RuntimeException {
    public CanalNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
