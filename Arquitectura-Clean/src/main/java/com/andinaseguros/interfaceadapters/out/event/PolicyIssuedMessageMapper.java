package com.andinaseguros.interfaceadapters.out.event;

import com.andinaseguros.entities.event.PolizaEmitidaEvent;

public class PolicyIssuedMessageMapper {
    public PolicyIssuedMessage toMessage(PolizaEmitidaEvent event) {
        return new PolicyIssuedMessage(
                event.eventId(),
                "PolicyIssued",
                1,
                event.occurredAt(),
                event.polizaId(),
                new PolicyIssuedMessage.PolicyIssuedData(
                        event.polizaId(),
                        event.cotizacionId(),
                        event.clienteId(),
                        event.numeroPoliza()));
    }
}
