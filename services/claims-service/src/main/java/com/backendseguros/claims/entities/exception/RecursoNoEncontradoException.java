package com.backendseguros.claims.entities.exception;

public class RecursoNoEncontradoException extends DomainException {
    public RecursoNoEncontradoException(String recurso) {
        super("RECURSO_NO_ENCONTRADO", recurso + " no encontrado");
    }
}
