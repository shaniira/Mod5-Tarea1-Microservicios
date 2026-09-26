package com.andinaseguros.interfaceadapters.in.rest.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CrearFactorRequest(
        @NotBlank String codigo,
        @NotBlank String nombre,
        @NotBlank String tipoVariable,
        @NotNull BigDecimal valorMinimo,
        @NotNull BigDecimal valorMaximo,
        @NotNull @DecimalMin("0.01") BigDecimal multiplicador,
        @Min(1) int orden) {}
