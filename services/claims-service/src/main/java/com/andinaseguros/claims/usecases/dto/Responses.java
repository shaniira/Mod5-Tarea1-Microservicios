package com.andinaseguros.claims.usecases.dto;

import com.andinaseguros.claims.entities.enums.EstadoSiniestro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Misma respuesta que daba el monolito en /api/polizas/{id}/siniestros (el frontend no cambia). */
public final class Responses {
    private Responses() {}

    public record SiniestroResponse(
            UUID id,
            UUID polizaId,
            LocalDate fecha,
            String tipo,
            BigDecimal montoEstimado,
            boolean responsabilidadAsegurado,
            String gravedad,
            EstadoSiniestro estado) {}

    /** Resultado del backfill de claim.registered.v1. */
    public record ReenvioEventosResponse(int siniestrosPublicados) {}
}
