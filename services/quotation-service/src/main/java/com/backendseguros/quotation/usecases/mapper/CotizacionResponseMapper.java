package com.backendseguros.quotation.usecases.mapper;

import com.backendseguros.quotation.usecases.dto.Responses.CotizacionResponse;
import com.backendseguros.quotation.entities.model.*;

public final class CotizacionResponseMapper {
    private CotizacionResponseMapper() {}

    public static CotizacionResponse toResponse(
            Cotizacion cotizacion, ResultadoTarificacion desglose) {
        return new CotizacionResponse(
                cotizacion.getId(),
                cotizacion.getNumero(),
                cotizacion.getClienteId(),
                cotizacion.getVehiculoId(),
                cotizacion.getPrima().valor(),
                cotizacion.getPrima().moneda(),
                cotizacion.getFechaCreacion(),
                cotizacion.getFechaExpiracion(),
                cotizacion.getEstado(),
                desglose != null ? desglose : cotizacion.getDesglose());
    }
}
