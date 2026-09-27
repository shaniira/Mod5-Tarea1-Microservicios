package com.andinaseguros.quotation.entities.event;

import com.andinaseguros.quotation.entities.model.ResultadoTarificacion;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/** quote.accepted.v1: todo lo que policy-service necesita para emitir sin consultar a quotation. */
public record CotizacionAceptadaEvent(
        UUID eventId,
        Instant occurredAt,
        UUID cotizacionId,
        String numero,
        UUID clienteId,
        UUID vehiculoId,
        BigDecimal prima,
        String moneda,
        UUID tablaTarifariaId,
        LocalDateTime creada,
        LocalDateTime expira,
        ResultadoTarificacion desglose)
        implements DomainEvent {}
