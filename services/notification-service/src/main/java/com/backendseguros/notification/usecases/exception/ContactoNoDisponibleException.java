package com.backendseguros.notification.usecases.exception;

/**
 * La proyección no tiene el contacto del cliente (o no tiene teléfono). Se reintenta unas veces por
 * si el evento customer.* aún no llegó; si sigue faltando, el mensaje va a la DLQ y se reprocesa
 * después del backfill.
 */
public class ContactoNoDisponibleException extends RuntimeException {
    public ContactoNoDisponibleException(String message) {
        super(message);
    }
}
