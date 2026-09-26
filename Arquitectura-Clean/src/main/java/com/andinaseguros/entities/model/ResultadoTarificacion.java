package com.andinaseguros.entities.model;

import com.andinaseguros.entities.valueobject.Dinero;
import java.math.BigDecimal;
import java.util.List;

public record ResultadoTarificacion(
        Dinero primaBase,
        Dinero primaRiesgo,
        Dinero gastos,
        Dinero recargos,
        Dinero descuentos,
        Dinero primaComercial,
        List<FactorAplicado> factores) {
    public record FactorAplicado(
            String codigo, String nombre, BigDecimal valorEvaluado, BigDecimal multiplicador) {}
}
