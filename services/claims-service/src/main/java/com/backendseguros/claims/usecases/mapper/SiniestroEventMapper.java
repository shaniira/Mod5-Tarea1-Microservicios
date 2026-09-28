package com.backendseguros.claims.usecases.mapper;

import com.backendseguros.claims.entities.enums.EstadoSiniestro;
import com.backendseguros.claims.entities.event.SiniestroEstadoCambiadoEvent;
import com.backendseguros.claims.entities.event.SiniestroRegistradoEvent;
import com.backendseguros.claims.entities.model.Siniestro;
import java.time.Instant;
import java.util.UUID;

public final class SiniestroEventMapper {
    private SiniestroEventMapper() {}

    public static SiniestroRegistradoEvent registrado(
            UUID eventId, Instant ahora, Siniestro siniestro, UUID clienteId) {
        return new SiniestroRegistradoEvent(
                eventId,
                ahora,
                siniestro.id(),
                siniestro.polizaId(),
                clienteId,
                siniestro.estado(),
                siniestro.estaAbierto(),
                siniestro.fecha(),
                siniestro.tipo(),
                siniestro.montoEstimado().valor(),
                siniestro.montoEstimado().moneda(),
                siniestro.responsabilidadAsegurado(),
                siniestro.gravedad(),
                siniestro.version());
    }

    public static SiniestroEstadoCambiadoEvent estadoCambiado(
            UUID eventId, Instant ahora, Siniestro siniestro, EstadoSiniestro estadoAnterior) {
        return new SiniestroEstadoCambiadoEvent(
                eventId,
                ahora,
                siniestro.id(),
                siniestro.polizaId(),
                estadoAnterior,
                siniestro.estado(),
                siniestro.estaAbierto(),
                siniestro.responsabilidadAsegurado(),
                siniestro.version());
    }
}
