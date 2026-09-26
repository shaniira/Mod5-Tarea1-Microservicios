package com.andinaseguros.usecases.mapper;

import com.andinaseguros.usecases.dto.Responses.PolizaResponse;
import com.andinaseguros.entities.model.Poliza;

public final class PolizaResponseMapper {
    private PolizaResponseMapper() {}

    public static PolizaResponse toResponse(Poliza poliza) {
        return new PolizaResponse(
                poliza.getId(),
                poliza.getNumero(),
                poliza.getCotizacionId(),
                poliza.getClienteId(),
                poliza.getVehiculoId(),
                poliza.getPrima().valor(),
                poliza.getPrima().moneda(),
                poliza.getVigencia().inicio(),
                poliza.getVigencia().fin(),
                poliza.getEstado(),
                poliza.getRenovacionOrigenId());
    }
}
