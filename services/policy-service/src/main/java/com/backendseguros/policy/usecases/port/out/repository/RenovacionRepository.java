package com.backendseguros.policy.usecases.port.out.repository;

import com.backendseguros.policy.entities.model.PropuestaRenovacion;
import java.util.*;

public interface RenovacionRepository {
    PropuestaRenovacion guardar(PropuestaRenovacion propuesta);

    Optional<PropuestaRenovacion> buscarPorId(UUID id);

    List<PropuestaRenovacion> listar();

    List<PropuestaRenovacion> listarPorPoliza(UUID polizaId);
}
