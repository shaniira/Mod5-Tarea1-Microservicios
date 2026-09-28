package com.backendseguros.identity.entities.exception;

public class DomainException extends RuntimeException {
    private final String codigo;

    public DomainException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
