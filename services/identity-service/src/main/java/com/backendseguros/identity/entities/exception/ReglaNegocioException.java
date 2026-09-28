package com.backendseguros.identity.entities.exception;

public class ReglaNegocioException extends DomainException {
    public ReglaNegocioException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
