package com.andinaseguros.policy.usecases.port.out.repository;

import com.andinaseguros.policy.entities.model.PropuestaRenovacion;
import java.util.*;

public interface RenovacionRepository {
    PropuestaRenovacion guardar(PropuestaRenovacion propuesta);

    Optional<PropuestaRenovacion> buscarPorId(UUID id);

    List<PropuestaRenovacion> listar();

    List<PropuestaRenovacion> listarPorPoliza(UUID polizaId);
}
