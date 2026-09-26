package com.andinaseguros.interfaceadapters.in.rest.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CrearCotizacionRequest(
        @NotNull UUID clienteId,
        @NotNull UUID vehiculoId,
        @Min(0) int siniestrosResponsables,
        @DecimalMin("0.0") BigDecimal porcentajeGastos,
        @DecimalMin("0.0") BigDecimal porcentajeRecargo,
        @DecimalMin("0.0") @DecimalMax("0.5") BigDecimal porcentajeDescuento) {}
