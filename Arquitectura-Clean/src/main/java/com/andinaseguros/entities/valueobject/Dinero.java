package com.andinaseguros.entities.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Dinero(BigDecimal valor, String moneda) {
    public Dinero {
        Objects.requireNonNull(valor, "El valor es obligatorio");
        Objects.requireNonNull(moneda, "La moneda es obligatoria");
        if (valor.signum() < 0)
            throw new IllegalArgumentException("El dinero no puede ser negativo");
        valor = valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static Dinero soles(BigDecimal valor) {
        return new Dinero(valor, "PEN");
    }

    public Dinero multiplicar(BigDecimal factor) {
        return new Dinero(valor.multiply(factor), moneda);
    }

    public Dinero sumar(Dinero otro) {
        validarMoneda(otro);
        return new Dinero(valor.add(otro.valor), moneda);
    }

    public Dinero restar(Dinero otro) {
        validarMoneda(otro);
        return new Dinero(valor.subtract(otro.valor).max(BigDecimal.ZERO), moneda);
    }

    private void validarMoneda(Dinero otro) {
        if (!moneda.equals(otro.moneda))
            throw new IllegalArgumentException("Monedas incompatibles");
    }
}
