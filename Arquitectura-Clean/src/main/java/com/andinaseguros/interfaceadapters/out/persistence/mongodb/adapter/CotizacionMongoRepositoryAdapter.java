package com.andinaseguros.interfaceadapters.out.persistence.mongodb.adapter;

import com.andinaseguros.entities.enums.EstadoCotizacion;
import com.andinaseguros.entities.model.Cotizacion;
import com.andinaseguros.usecases.port.out.repository.CotizacionRepository;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.mapper.CotizacionMongoMapper;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.repository.SpringDataCotizacionMongoRepository;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class CotizacionMongoRepositoryAdapter implements CotizacionRepository {
    private final SpringDataCotizacionMongoRepository repo;
    private final CotizacionMongoMapper mapper;

    public CotizacionMongoRepositoryAdapter(
            SpringDataCotizacionMongoRepository r, CotizacionMongoMapper m) {
        repo = r;
        mapper = m;
    }

    public Cotizacion guardar(Cotizacion x) {
        return mapper.toDomain(repo.save(mapper.toDocument(x)));
    }

    public Optional<Cotizacion> buscarPorId(UUID id) {
        return repo.findById(id.toString()).map(mapper::toDomain);
    }

    public List<Cotizacion> listar() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "fechaCreacion")).stream()
                .map(mapper::toDomain)
                .toList();
    }

    public List<Cotizacion> listarPorEstado(EstadoCotizacion e) {
        return repo.findByEstadoOrderByFechaCreacionDesc(e).stream().map(mapper::toDomain).toList();
    }

    public List<Cotizacion> listarPorCliente(UUID id) {
        return repo.findByClienteId(id.toString()).stream().map(mapper::toDomain).toList();
    }
}
