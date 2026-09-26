package com.andinaseguros.interfaceadapters.out.persistence.mongodb.adapter;

import com.andinaseguros.entities.model.Vehiculo;
import com.andinaseguros.usecases.port.out.repository.VehiculoRepository;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.mapper.VehiculoMongoMapper;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.repository.SpringDataVehiculoMongoRepository;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class VehiculoMongoRepositoryAdapter implements VehiculoRepository {
    private final SpringDataVehiculoMongoRepository repo;
    private final VehiculoMongoMapper mapper;

    public VehiculoMongoRepositoryAdapter(
            SpringDataVehiculoMongoRepository r, VehiculoMongoMapper m) {
        repo = r;
        mapper = m;
    }

    public Vehiculo guardar(Vehiculo x) {
        return mapper.toDomain(repo.save(mapper.toDocument(x)));
    }

    public Optional<Vehiculo> buscarPorId(UUID id) {
        return repo.findById(id.toString()).map(mapper::toDomain);
    }

    public Optional<Vehiculo> buscarPorPlaca(String x) {
        return repo.findByPlaca(x).map(mapper::toDomain);
    }

    public List<Vehiculo> listarPorCliente(UUID id) {
        return repo.findByClienteId(id.toString()).stream().map(mapper::toDomain).toList();
    }
}
