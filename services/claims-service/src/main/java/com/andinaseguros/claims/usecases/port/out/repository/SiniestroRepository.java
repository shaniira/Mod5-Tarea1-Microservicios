package com.andinaseguros.claims.usecases.port.out.repository;

import com.andinaseguros.claims.entities.model.Siniestro;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SiniestroRepository {
    Siniestro guardar(Siniestro siniestro);

    Optional<Siniestro> buscarPorId(UUID id);

    List<Siniestro> listarPorPoliza(UUID polizaId);

    List<Siniestro> listar();
}
