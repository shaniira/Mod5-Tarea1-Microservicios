package com.andinaseguros.usecases.mapper;

import com.andinaseguros.usecases.dto.Responses.RenovacionResponse;
import com.andinaseguros.entities.model.PropuestaRenovacion;

public final class RenovacionResponseMapper {
    private RenovacionResponseMapper() {}

    public static RenovacionResponse toResponse(PropuestaRenovacion propuesta) {
        return new RenovacionResponse(
                propuesta.id(),
                propuesta.polizaOrigenId(),
                propuesta.primaAnterior().valor(),
                propuesta.nuevaPrima().valor(),
                propuesta.porcentajeVariacion(),
                propuesta.siniestrosConsiderados(),
                propuesta.estado(),
                propuesta.motivo(),
                propuesta.creadaEn(),
                propuesta.venceEn(),
                propuesta.decididaEn(),
                propuesta.polizaRenovadaId());
    }
}
