package com.andinaseguros.usecases.dto;

import java.math.BigDecimal;

public record CrearFactorRequestModel(
        String codigo,
        String nombre,
        String tipoVariable,
        BigDecimal valorMinimo,
        BigDecimal valorMaximo,
        BigDecimal multiplicador,
        int orden) {}
