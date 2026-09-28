package com.backendseguros.policy.entities.model;

import com.backendseguros.policy.entities.valueobject.Dinero;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cotización aceptada (proyección accepted_quotes, paso 6.2), copiada de quote.accepted.v1: todo lo
 * que hace falta para emitir sin consultar a quotation.
 */
public record CotizacionAceptada(
        UUID cotizacionId,
        String numero,
        UUID clienteId,
        UUID vehiculoId,
        Dinero prima,
        LocalDateTime expira) {

    public CotizacionAceptada {
        if (expira == null) {
            throw new IllegalArgumentException("Una cotización aceptada debe tener fecha de expiración");
        }
    }

    public boolean vencidaEn(LocalDateTime ahora) {
        return ahora.isAfter(expira);
    }
}
