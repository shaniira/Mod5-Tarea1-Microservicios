package com.andinaseguros.policy.interfaceadapters.out.persistence.mongodb.adapter;

import com.andinaseguros.policy.entities.model.Poliza;
import com.andinaseguros.policy.interfaceadapters.out.persistence.mongodb.mapper.PolizaMongoMapper;
import com.andinaseguros.policy.interfaceadapters.out.persistence.mongodb.repository.SpringDataPolizaMongoRepository;
import com.andinaseguros.policy.usecases.exception.CotizacionYaEmitidaException;
import com.andinaseguros.policy.usecases.port.out.repository.PolizaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
public class PolizaMongoRepositoryAdapter implements PolizaRepository {
    private final SpringDataPolizaMongoRepository repo;
    private final PolizaMongoMapper mapper;

    public PolizaMongoRepositoryAdapter(SpringDataPolizaMongoRepository repo, PolizaMongoMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    /** El índice único parcial de cotizacionId convierte una doble emisión en un error de negocio. */
    public Poliza guardar(Poliza poliza) {
        try {
            return mapper.toDomain(repo.save(mapper.toDocument(poliza)));
        } catch (DuplicateKeyException duplicada) {
            if (duplicada.getMessage() != null && duplicada.getMessage().contains("cotizacionId")) {
                throw new CotizacionYaEmitidaException();
            }
            throw duplicada;
        }
    }

    public Optional<Poliza> buscarPorId(UUID id) {
        return repo.findById(id.toString()).map(mapper::toDomain);
    }

    public Optional<Poliza> buscarPorCotizacionId(UUID cotizacionId) {
        return repo.findByCotizacionId(cotizacionId.toString()).map(mapper::toDomain);
    }

    public List<Poliza> listar() {
        return repo.findAll().stream().map(mapper::toDomain).toList();
    }

    public List<Poliza> listarPorCliente(UUID clienteId) {
        return repo.findByClienteId(clienteId.toString()).stream().map(mapper::toDomain).toList();
    }
}
