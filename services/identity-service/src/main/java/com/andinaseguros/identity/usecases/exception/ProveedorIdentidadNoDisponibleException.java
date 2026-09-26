package com.andinaseguros.identity.usecases.exception;

/**
 * Google o Facebook no responden (o su circuit breaker está abierto). A diferencia de un token
 * inválido, no es culpa del usuario: se le pide entrar con contraseña o reintentar más tarde.
 */
public class ProveedorIdentidadNoDisponibleException extends RuntimeException {
    public ProveedorIdentidadNoDisponibleException(String proveedor, Throwable cause) {
        super(proveedor + " no disponible", cause);
    }
}
