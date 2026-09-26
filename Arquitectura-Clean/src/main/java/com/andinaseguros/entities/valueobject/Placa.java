package com.andinaseguros.entities.valueobject;

public record Placa(String valor) {
    public Placa {
        if (valor == null || !valor.toUpperCase().matches("[A-Z0-9-]{5,8}")) {
            throw new IllegalArgumentException("Placa inválida");
        }
        valor = valor.toUpperCase();
    }
}
