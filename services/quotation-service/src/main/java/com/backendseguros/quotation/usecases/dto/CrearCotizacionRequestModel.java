package com.backendseguros.quotation.usecases.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CrearCotizacionRequestModel(
        UUID clienteId,
        UUID vehiculoId,
        int siniestrosResponsables,
        BigDecimal porcentajeGastos,
        BigDecimal porcentajeRecargo,
        BigDecimal porcentajeDescuento) {}
