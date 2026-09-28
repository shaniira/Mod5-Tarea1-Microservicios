package com.backendseguros.policy.entities.event;

import com.backendseguros.policy.entities.enums.EstadoPoliza;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** policy.issued.v1: mismos campos que publicaba el backend, más los opcionales de la fase 6. */
public record PolizaEmitidaEvent(
        UUID eventId,
        Instant occurredAt,
        UUID polizaId,
        UUID cotizacionId,
        UUID clienteId,
        String numeroPoliza,
        UUID vehiculoId,
        BigDecimal prima,
        String moneda,
        LocalDate inicio,
        LocalDate fin,
        EstadoPoliza estado,
        long version)
        implements DomainEvent {}
