package com.andinaseguros.policy.entities.event;

import java.time.Instant;
import java.util.UUID;

/** policy.issuance-rejected.v1: compensación de la saga de emisión (paso 6.4). */
public record EmisionRechazadaEvent(
        UUID eventId,
        Instant occurredAt,
        UUID cotizacionId,
        String codigoMotivo,
        String motivo,
        UUID polizaExistenteId)
        implements DomainEvent {}
