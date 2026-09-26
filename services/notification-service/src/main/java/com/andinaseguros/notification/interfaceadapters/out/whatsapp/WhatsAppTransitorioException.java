package com.andinaseguros.notification.interfaceadapters.out.whatsapp;

/**
 * Fallo que puede desaparecer solo (timeout, conexión rechazada, 5xx, 429). Es la única excepción
 * que cuenta como fallo para el circuit breaker "whatsapp" y la única que se reintenta (ver
 * application.yml).
 */
public class WhatsAppTransitorioException extends RuntimeException {
    public WhatsAppTransitorioException(String message, Throwable cause) {
        super(message, cause);
    }
}
