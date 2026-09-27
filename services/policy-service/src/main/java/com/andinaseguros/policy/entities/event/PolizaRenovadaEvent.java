package com.andinaseguros.policy.entities.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** policy.renewed.v1: la póliza nueva queda VIGENTE y la anterior RENOVADA. */
public record PolizaRenovadaEvent(
        UUID eventId,
        Instant occurredAt,
        UUID polizaNuevaId,
        UUID polizaAnteriorId,
        UUID renovacionId,
        UUID clienteId,
        UUID vehiculoId,
        String numeroPolizaNueva,
        BigDecimal prima,
        String moneda,
        LocalDate inicio,
        LocalDate fin,
        long versionPolizaAnterior)
        implements DomainEvent {}
