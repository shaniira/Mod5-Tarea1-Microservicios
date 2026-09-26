package com.andinaseguros.entities.event;

import java.time.Instant;
import java.util.UUID;

public record PolizaEmitidaEvent(
        UUID eventId,
        Instant occurredAt,
        UUID polizaId,
        UUID cotizacionId,
        UUID clienteId,
        String numeroPoliza)
        implements DomainEvent {}
