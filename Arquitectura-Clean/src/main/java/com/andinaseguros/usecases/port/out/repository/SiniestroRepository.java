package com.andinaseguros.usecases.port.out.repository;

import com.andinaseguros.entities.model.Siniestro;
import java.util.*;

public interface SiniestroRepository {
    Siniestro guardar(Siniestro siniestro);

    Optional<Siniestro> buscarPorId(UUID id);

    List<Siniestro> listarPorPoliza(UUID polizaId);
}
