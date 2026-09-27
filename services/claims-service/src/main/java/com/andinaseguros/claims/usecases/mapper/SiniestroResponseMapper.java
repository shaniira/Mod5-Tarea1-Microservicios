package com.andinaseguros.claims.usecases.mapper;

import com.andinaseguros.claims.usecases.dto.Responses.SiniestroResponse;
import com.andinaseguros.claims.entities.model.Siniestro;

public final class SiniestroResponseMapper {
    private SiniestroResponseMapper() {}

    public static SiniestroResponse toResponse(Siniestro siniestro) {
        return new SiniestroResponse(
                siniestro.id(),
                siniestro.polizaId(),
                siniestro.fecha(),
                siniestro.tipo(),
                siniestro.montoEstimado().valor(),
                siniestro.responsabilidadAsegurado(),
                siniestro.gravedad(),
                siniestro.estado());
    }
}
