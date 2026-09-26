package com.andinaseguros.interfaceadapters.out.persistence.mongodb.adapter;

import com.andinaseguros.entities.model.FactorRiesgo;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.mapper.FactorRiesgoMongoMapper;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.repository.SpringDataFactorRiesgoMongoRepository;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class FactorRiesgoMongoRepositoryAdapter {
    private final SpringDataFactorRiesgoMongoRepository repo;
    private final FactorRiesgoMongoMapper mapper;

    public FactorRiesgoMongoRepositoryAdapter(
            SpringDataFactorRiesgoMongoRepository r, FactorRiesgoMongoMapper m) {
        repo = r;
        mapper = m;
    }

    public List<FactorRiesgo> guardarTodos(List<FactorRiesgo> factores) {
        return repo.saveAll(factores.stream().map(mapper::toDocument).toList()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
