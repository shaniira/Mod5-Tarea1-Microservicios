package com.andinaseguros.interfaceadapters.in.rest.request;

import com.andinaseguros.entities.enums.EstadoTablaTarifaria;
import com.andinaseguros.entities.enums.TipoUso;
import com.andinaseguros.entities.enums.TipoVehiculo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CrearTablaRequest(
        @NotBlank String codigo,
        @Min(1) int version,
        @NotNull TipoVehiculo tipoVehiculo,
        @NotNull TipoUso tipoUso,
        @NotNull @Positive BigDecimal primaBase,
        @NotNull @Positive BigDecimal primaMinima,
        @NotNull LocalDate inicioVigencia,
        @NotNull LocalDate finVigencia,
        @NotBlank String codigoNotaTecnica,
        @NotNull EstadoTablaTarifaria estado,
        List<CrearFactorRequest> factores) {}
