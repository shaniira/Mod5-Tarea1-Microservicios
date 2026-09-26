package com.andinaseguros.interfaceadapters.out.persistence.mongodb.adapter;

import com.andinaseguros.entities.model.Siniestro;
import com.andinaseguros.usecases.port.out.repository.SiniestroRepository;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.mapper.SiniestroMongoMapper;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.repository.SpringDataSiniestroMongoRepository;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class SiniestroMongoRepositoryAdapter implements SiniestroRepository {
    private final SpringDataSiniestroMongoRepository repo;
    private final SiniestroMongoMapper mapper;

    public SiniestroMongoRepositoryAdapter(
            SpringDataSiniestroMongoRepository r, SiniestroMongoMapper m) {
        repo = r;
        mapper = m;
    }

    public Siniestro guardar(Siniestro x) {
        return mapper.toDomain(repo.save(mapper.toDocument(x)));
    }

    public Optional<Siniestro> buscarPorId(UUID id) {
        return repo.findById(id.toString()).map(mapper::toDomain);
    }

    public List<Siniestro> listarPorPoliza(UUID id) {
        return repo.findByPolizaId(id.toString()).stream().map(mapper::toDomain).toList();
    }
}
