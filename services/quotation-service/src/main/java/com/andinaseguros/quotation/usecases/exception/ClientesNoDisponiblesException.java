package com.andinaseguros.quotation.usecases.exception;

import com.andinaseguros.quotation.entities.exception.DomainException;

/**
 * El cliente o el vehículo no están en la proyección y customer-service no respondió (paso 5.3).
 * Se responde 503 con un mensaje claro para reintentar, nunca un 500.
 */
public class ClientesNoDisponiblesException extends DomainException {
    public static final String CODIGO = "CLIENTES_NO_DISPONIBLE";

    public ClientesNoDisponiblesException(Throwable causa) {
        super(
                CODIGO,
                "No se puede validar el cliente o el vehículo en este momento. Intenta de nuevo en unos segundos.");
        initCause(causa);
    }
}
