package com.backendseguros.policy.usecases.port.out.repository;

import com.backendseguros.policy.entities.model.SiniestroRef;
import java.util.List;
import java.util.UUID;

/** Proyección claim_ref (paso 6.2): reemplaza a SiniestroRepository del monolito. */
public interface SiniestrosRefRepository {
    List<SiniestroRef> listarPorPoliza(UUID polizaId);

    /** Guarda si la versión es mayor que la guardada; false si era igual o más vieja. */
    boolean guardarSiEsMasNuevo(SiniestroRef siniestro, long version);
}
