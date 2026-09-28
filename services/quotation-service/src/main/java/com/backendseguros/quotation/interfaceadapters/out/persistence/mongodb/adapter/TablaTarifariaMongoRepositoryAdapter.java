package com.backendseguros.quotation.interfaceadapters.out.persistence.mongodb.adapter;

import com.backendseguros.quotation.entities.enums.EstadoTablaTarifaria;
import com.backendseguros.quotation.entities.enums.TipoUso;
import com.backendseguros.quotation.entities.enums.TipoVehiculo;
import com.backendseguros.quotation.entities.model.TablaTarifaria;
import com.backendseguros.quotation.interfaceadapters.out.persistence.mongodb.mapper.TablaTarifariaMongoMapper;
import com.backendseguros.quotation.interfaceadapters.out.persistence.mongodb.repository.SpringDataTablaTarifariaMongoRepository;
import com.backendseguros.quotation.usecases.port.out.repository.TablaTarifariaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Los factores se guardan dentro de la tabla (como ya hacía el monolito). La copia aparte en la
 * colección factores_riesgo del monolito no se lee en ningún lado, así que no se trae.
 */
@Repository
public class TablaTarifariaMongoRepositoryAdapter implements TablaTarifariaRepository {
    private final SpringDataTablaTarifariaMongoRepository repo;
    private final TablaTarifariaMongoMapper mapper;

    public TablaTarifariaMongoRepositoryAdapter(
            SpringDataTablaTarifariaMongoRepository repo, TablaTarifariaMongoMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public TablaTarifaria guardar(TablaTarifaria tabla) {
        return mapper.toDomain(repo.save(mapper.toDocument(tabla)));
    }

    public Optional<TablaTarifaria> buscarPorId(UUID id) {
        return repo.findById(id.toString()).map(mapper::toDomain);
    }

    public Optional<TablaTarifaria> buscarVigente(TipoVehiculo tipo, TipoUso uso, LocalDate fecha) {
        return repo
                .findByTipoVehiculoAndTipoUsoAndEstadoAndInicioVigenciaLessThanEqualAndFinVigenciaGreaterThanEqualOrderByVersionDesc(
                        tipo, uso, EstadoTablaTarifaria.VIGENTE, fecha, fecha)
                .stream()
                .findFirst()
                .map(mapper::toDomain);
    }

    public List<TablaTarifaria> listar() {
        return repo.findAll().stream().map(mapper::toDomain).toList();
    }
}
