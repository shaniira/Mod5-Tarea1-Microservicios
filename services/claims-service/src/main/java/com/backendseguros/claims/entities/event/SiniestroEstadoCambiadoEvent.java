package com.backendseguros.claims.entities.event;

import com.backendseguros.claims.entities.enums.EstadoSiniestro;
import java.time.Instant;
import java.util.UUID;

/** claim.status-changed.v1. */
public record SiniestroEstadoCambiadoEvent(
        UUID eventId,
        Instant occurredAt,
        UUID siniestroId,
        UUID polizaId,
        EstadoSiniestro estadoAnterior,
        EstadoSiniestro estadoNuevo,
        boolean abierto,
        boolean responsabilidadAsegurado,
        long version)
        implements DomainEvent {}
