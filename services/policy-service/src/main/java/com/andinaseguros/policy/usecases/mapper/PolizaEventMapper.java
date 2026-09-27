package com.andinaseguros.policy.usecases.mapper;

import com.andinaseguros.policy.entities.event.PolizaEmitidaEvent;
import com.andinaseguros.policy.entities.event.PolizaRenovadaEvent;
import com.andinaseguros.policy.entities.model.Poliza;
import java.time.Instant;
import java.util.UUID;

public final class PolizaEventMapper {
    private PolizaEventMapper() {}

    public static PolizaEmitidaEvent emitida(UUID eventId, Instant ahora, Poliza poliza) {
        return new PolizaEmitidaEvent(
                eventId,
                ahora,
                poliza.getId(),
                poliza.getCotizacionId(),
                poliza.getClienteId(),
                poliza.getNumero(),
                poliza.getVehiculoId(),
                poliza.getPrima().valor(),
                poliza.getPrima().moneda(),
                poliza.getVigencia().inicio(),
                poliza.getVigencia().fin(),
                poliza.getEstado(),
                poliza.getVersion());
    }

    public static PolizaRenovadaEvent renovada(
            UUID eventId, Instant ahora, Poliza nueva, Poliza anterior, UUID renovacionId) {
        return new PolizaRenovadaEvent(
                eventId,
                ahora,
                nueva.getId(),
                anterior.getId(),
                renovacionId,
                nueva.getClienteId(),
                nueva.getVehiculoId(),
                nueva.getNumero(),
                nueva.getPrima().valor(),
                nueva.getPrima().moneda(),
                nueva.getVigencia().inicio(),
                nueva.getVigencia().fin(),
                anterior.getVersion());
    }
}
