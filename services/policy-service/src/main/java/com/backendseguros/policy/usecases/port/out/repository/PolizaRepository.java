package com.backendseguros.policy.usecases.port.out.repository;

import com.backendseguros.policy.entities.model.Poliza;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PolizaRepository {
    /**
     * Guarda la póliza. Si ya existe otra póliza de la misma cotización (índice único por
     * cotizacionId, paso 6.3) lanza CotizacionYaEmitidaException, aunque las dos emisiones lleguen
     * a la vez desde dos réplicas.
     */
    Poliza guardar(Poliza poliza);

    Optional<Poliza> buscarPorId(UUID id);

    Optional<Poliza> buscarPorCotizacionId(UUID cotizacionId);

    List<Poliza> listar();

    List<Poliza> listarPorCliente(UUID clienteId);
}
