package com.backendseguros.policy.usecases.mapper;

import com.backendseguros.policy.usecases.dto.Responses.RenovacionResponse;
import com.backendseguros.policy.entities.model.PropuestaRenovacion;

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
