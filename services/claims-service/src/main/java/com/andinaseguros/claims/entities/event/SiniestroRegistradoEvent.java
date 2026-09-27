package com.andinaseguros.claims.entities.event;

import com.andinaseguros.claims.entities.enums.EstadoSiniestro;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** claim.registered.v1 (también es el snapshot del backfill, con el estado y la versión actuales). */
public record SiniestroRegistradoEvent(
        UUID eventId,
        Instant occurredAt,
        UUID siniestroId,
        UUID polizaId,
        UUID clienteId,
        EstadoSiniestro estado,
        boolean abierto,
        LocalDate fecha,
        String tipo,
        BigDecimal montoEstimado,
        String moneda,
        boolean responsabilidadAsegurado,
        String gravedad,
        long version)
        implements DomainEvent {}
