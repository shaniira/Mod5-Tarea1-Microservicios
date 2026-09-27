package com.andinaseguros.quotation.usecases.dto;

import com.andinaseguros.quotation.entities.enums.EstadoCotizacion;
import com.andinaseguros.quotation.entities.enums.EstadoTablaTarifaria;
import com.andinaseguros.quotation.entities.enums.TipoUso;
import com.andinaseguros.quotation.entities.enums.TipoVehiculo;
import com.andinaseguros.quotation.entities.model.ResultadoTarificacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Mismas respuestas que el monolito en /api/tablas-tarifarias y /api/cotizaciones. */
public final class Responses {
    private Responses() {}

    public record TablaResponse(
            UUID id,
            String codigo,
            int version,
            TipoVehiculo tipoVehiculo,
            TipoUso tipoUso,
            BigDecimal primaBase,
            BigDecimal primaMinima,
            LocalDate inicio,
            LocalDate fin,
            String notaTecnica,
            EstadoTablaTarifaria estado) {}

    public record FactorResponse(
            UUID id,
            String codigo,
            String nombre,
            String tipoVariable,
            BigDecimal valorMinimo,
            BigDecimal valorMaximo,
            BigDecimal multiplicador,
            int orden) {}

    public record TablaDetalleResponse(
            UUID id,
            String codigo,
            int version,
            TipoVehiculo tipoVehiculo,
            TipoUso tipoUso,
            BigDecimal primaBase,
            BigDecimal primaMinima,
            LocalDate inicio,
            LocalDate fin,
            String notaTecnica,
            EstadoTablaTarifaria estado,
            List<FactorResponse> factores) {}

    public record CotizacionResponse(
            UUID id,
            String numero,
            UUID clienteId,
            UUID vehiculoId,
            BigDecimal prima,
            String moneda,
            LocalDateTime creada,
            LocalDateTime expira,
            EstadoCotizacion estado,
            ResultadoTarificacion desglose) {}
}
