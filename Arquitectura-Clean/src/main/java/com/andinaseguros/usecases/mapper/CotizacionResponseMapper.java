package com.andinaseguros.usecases.mapper;

import com.andinaseguros.usecases.dto.Responses.CotizacionResponse;
import com.andinaseguros.entities.model.*;

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
