package com.andinaseguros.entities.service;

import com.andinaseguros.entities.valueobject.Dinero;
import java.math.*;

public class CalculadorPrimaRenovacion {
    public record Resultado(Dinero nuevaPrima, BigDecimal porcentajeVariacion) {}

    public Resultado calcular(Dinero anterior, int responsables) {
        BigDecimal factor =
                BigDecimal.ONE.add(
                        BigDecimal.valueOf(Math.pow(responsables, 1.35))
                                .multiply(new BigDecimal("0.08")));
        Dinero nueva = anterior.multiplicar(factor);
        BigDecimal variacion =
                nueva.valor()
                        .subtract(anterior.valor())
                        .divide(anterior.valor(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
        return new Resultado(nueva, variacion);
    }
}
