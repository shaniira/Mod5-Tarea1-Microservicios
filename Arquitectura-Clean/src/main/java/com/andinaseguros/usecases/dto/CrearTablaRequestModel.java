package com.andinaseguros.usecases.dto;

import com.andinaseguros.entities.enums.EstadoTablaTarifaria;
import com.andinaseguros.entities.enums.TipoUso;
import com.andinaseguros.entities.enums.TipoVehiculo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CrearTablaRequestModel(
        String codigo,
        int version,
        TipoVehiculo tipoVehiculo,
        TipoUso tipoUso,
        BigDecimal primaBase,
        BigDecimal primaMinima,
        LocalDate inicioVigencia,
        LocalDate finVigencia,
        String codigoNotaTecnica,
        EstadoTablaTarifaria estado,
        List<CrearFactorRequestModel> factores) {}
