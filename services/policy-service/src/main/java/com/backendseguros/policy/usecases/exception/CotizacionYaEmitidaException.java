package com.backendseguros.policy.usecases.exception;

import com.backendseguros.policy.entities.exception.ReglaNegocioException;

/** Otra póliza ya usa esa cotización (índice único por cotizacionId, paso 6.3). */
public class CotizacionYaEmitidaException extends ReglaNegocioException {
    public static final String CODIGO = "COTIZACION_YA_EMITIDA";

    public CotizacionYaEmitidaException() {
        super(CODIGO, "La cotización ya tiene una póliza emitida");
    }
}
