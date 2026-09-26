package com.andinaseguros.interfaceadapters.out.persistence.mongodb.adapter;

import com.andinaseguros.entities.model.PropuestaRenovacion;
import com.andinaseguros.usecases.port.out.repository.RenovacionRepository;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.mapper.RenovacionMongoMapper;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.repository.SpringDataRenovacionMongoRepository;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class RenovacionMongoRepositoryAdapter implements RenovacionRepository {
    private final SpringDataRenovacionMongoRepository repo;
    private final RenovacionMongoMapper mapper;

    public RenovacionMongoRepositoryAdapter(
            SpringDataRenovacionMongoRepository r, RenovacionMongoMapper m) {
        repo = r;
        mapper = m;
    }

    public PropuestaRenovacion guardar(PropuestaRenovacion x) {
        return mapper.toDomain(repo.save(mapper.toDocument(x)));
    }

    public Optional<PropuestaRenovacion> buscarPorId(UUID id) {
        return repo.findById(id.toString()).map(mapper::toDomain);
    }

    public List<PropuestaRenovacion> listar() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "creadaEn")).stream()
                .map(mapper::toDomain)
                .toList();
    }

    public List<PropuestaRenovacion> listarPorPoliza(UUID id) {
        return repo.findByPolizaOrigenIdOrderByCreadaEnDesc(id.toString()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
