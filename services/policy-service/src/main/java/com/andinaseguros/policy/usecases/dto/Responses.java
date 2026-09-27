package com.andinaseguros.policy.usecases.dto;

import com.andinaseguros.policy.entities.enums.EstadoPoliza;
import com.andinaseguros.policy.entities.enums.EstadoRenovacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Mismas respuestas que el monolito en /api/polizas, /api/renovaciones y "Mi cuenta". */
public final class Responses {
    private Responses() {}

    public record PolizaResponse(
            UUID id,
            String numero,
            UUID cotizacionId,
            UUID clienteId,
            UUID vehiculoId,
            BigDecimal prima,
            String moneda,
            LocalDate inicio,
            LocalDate fin,
            EstadoPoliza estado,
            UUID renovacionOrigenId) {}

    public record RenovacionResponse(
            UUID id,
            UUID polizaOrigenId,
            BigDecimal primaAnterior,
            BigDecimal nuevaPrima,
            BigDecimal porcentajeVariacion,
            int siniestrosConsiderados,
            EstadoRenovacion estado,
            String motivo,
            LocalDateTime creadaEn,
            LocalDateTime venceEn,
            LocalDateTime decididaEn,
            UUID polizaRenovadaId) {}

    /** La parte de "Mi cuenta" que es de policy-service (paso 6.9). */
    public record PolizaConRenovacionesResponse(
            PolizaResponse poliza, List<RenovacionResponse> renovaciones) {}
}
