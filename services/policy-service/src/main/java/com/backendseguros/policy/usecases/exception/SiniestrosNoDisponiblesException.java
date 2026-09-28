package com.backendseguros.policy.usecases.exception;

import com.backendseguros.policy.entities.exception.DomainException;

/**
 * claims-service no respondió al confirmar los siniestros antes de generar una póliza renovada.
 * Se responde 503 con Retry-After: se prefiere no renovar antes que renovar sin saber si hay
 * siniestros abiertos (falla cerrada, CP).
 */
public class SiniestrosNoDisponiblesException extends DomainException {
    public static final String CODIGO = "SINIESTROS_NO_DISPONIBLE";

    public SiniestrosNoDisponiblesException(Throwable causa) {
        super(
                CODIGO,
                "No se pueden confirmar los siniestros de la póliza en este momento. Intenta de nuevo en unos segundos.");
        initCause(causa);
    }
}
