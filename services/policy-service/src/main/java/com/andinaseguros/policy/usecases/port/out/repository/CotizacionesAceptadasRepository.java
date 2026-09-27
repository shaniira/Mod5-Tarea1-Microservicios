package com.andinaseguros.policy.usecases.port.out.repository;

import com.andinaseguros.policy.entities.model.CotizacionAceptada;
import java.util.Optional;
import java.util.UUID;

/** Proyección accepted_quotes (paso 6.2): reemplaza a CotizacionRepository del monolito. */
public interface CotizacionesAceptadasRepository {
    Optional<CotizacionAceptada> buscar(UUID cotizacionId);

    /** Una cotización se acepta una sola vez: si ya estaba, no se cambia. False si ya existía. */
    boolean registrarSiNoExiste(CotizacionAceptada cotizacion);
}
