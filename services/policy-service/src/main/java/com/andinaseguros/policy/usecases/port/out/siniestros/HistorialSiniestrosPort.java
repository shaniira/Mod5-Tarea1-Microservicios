package com.andinaseguros.policy.usecases.port.out.siniestros;

import com.andinaseguros.policy.entities.model.SiniestroRef;
import java.util.List;
import java.util.UUID;

/**
 * Siniestros de una póliza leídos de su fuente (claims-service), no de la copia local claim_ref.
 * Se usa solo en el paso irreversible de la renovación (generar la póliza): ahí se prefiere un
 * error antes que renovar con datos viejos (CP). Si claims-service no responde, lanza
 * {@link com.andinaseguros.policy.usecases.exception.SiniestrosNoDisponiblesException}.
 */
public interface HistorialSiniestrosPort {
    List<SiniestroRef> listarPorPoliza(UUID polizaId);
}
