package com.andinaseguros.entities.model;

import java.math.BigDecimal;
import java.util.UUID;

public record FactorRiesgo(
        UUID id,
        String codigo,
        String nombre,
        String tipoVariable,
        BigDecimal valorMinimo,
        BigDecimal valorMaximo,
        BigDecimal multiplicador,
        int orden) {
    public FactorRiesgo {
        if (multiplicador == null || multiplicador.signum() <= 0)
            throw new IllegalArgumentException("Multiplicador inválido");
    }

    public boolean aplica(BigDecimal valor) {
        return valor.compareTo(valorMinimo) >= 0 && valor.compareTo(valorMaximo) <= 0;
    }
}
