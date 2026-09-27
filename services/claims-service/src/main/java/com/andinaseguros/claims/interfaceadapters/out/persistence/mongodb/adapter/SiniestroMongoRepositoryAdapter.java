package com.andinaseguros.claims.interfaceadapters.out.persistence.mongodb.adapter;

import com.andinaseguros.claims.entities.model.Siniestro;
import com.andinaseguros.claims.interfaceadapters.out.persistence.mongodb.mapper.SiniestroMongoMapper;
import com.andinaseguros.claims.interfaceadapters.out.persistence.mongodb.repository.SpringDataSiniestroMongoRepository;
import com.andinaseguros.claims.usecases.port.out.repository.SiniestroRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class SiniestroMongoRepositoryAdapter implements SiniestroRepository {
    private final SpringDataSiniestroMongoRepository repo;
    private final SiniestroMongoMapper mapper;

    public SiniestroMongoRepositoryAdapter(SpringDataSiniestroMongoRepository repo, SiniestroMongoMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public Siniestro guardar(Siniestro siniestro) {
        return mapper.toDomain(repo.save(mapper.toDocument(siniestro)));
    }

    public Optional<Siniestro> buscarPorId(UUID id) {
        return repo.findById(id.toString()).map(mapper::toDomain);
    }

    public List<Siniestro> listarPorPoliza(UUID polizaId) {
        return repo.findByPolizaId(polizaId.toString()).stream().map(mapper::toDomain).toList();
    }

    public List<Siniestro> listar() {
        return repo.findAll().stream().map(mapper::toDomain).toList();
    }
}
