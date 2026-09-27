package com.andinaseguros.quotation.interfaceadapters.out.persistence.mongodb.adapter;

import com.andinaseguros.quotation.entities.model.ClienteRef;
import com.andinaseguros.quotation.entities.model.VehiculoRef;
import com.andinaseguros.quotation.interfaceadapters.out.persistence.mongodb.document.ClienteRefDocument;
import com.andinaseguros.quotation.interfaceadapters.out.persistence.mongodb.document.VehiculoRefDocument;
import com.andinaseguros.quotation.usecases.port.out.repository.ReferenciaClientesRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Upsert condicional y atómico (mismo patrón que identity y notification): solo coincide si el
 * documento no existe o tiene una versión menor; si ya hay una igual o mayor, el upsert choca con
 * el _id y MongoDB lo rechaza.
 */
public class MongoReferenciaClientesRepository implements ReferenciaClientesRepository {
    private final MongoTemplate mongo;
    private final Clock clock;

    public MongoReferenciaClientesRepository(MongoTemplate mongo, Clock clock) {
        this.mongo = mongo;
        this.clock = clock;
    }

    @Override
    public Optional<ClienteRef> buscarCliente(UUID clienteId) {
        return Optional.ofNullable(mongo.findById(clienteId.toString(), ClienteRefDocument.class))
                .map(d -> new ClienteRef(UUID.fromString(d.id), d.fechaNacimiento, d.activo));
    }

    @Override
    public Optional<VehiculoRef> buscarVehiculo(UUID vehiculoId) {
        return Optional.ofNullable(mongo.findById(vehiculoId.toString(), VehiculoRefDocument.class))
                .map(
                        d ->
                                new VehiculoRef(
                                        UUID.fromString(d.id),
                                        UUID.fromString(d.clienteId),
                                        d.tipo,
                                        d.uso,
                                        d.anioFabricacion));
    }

    @Override
    public boolean guardarClienteSiEsMasNuevo(ClienteRef cliente, long version) {
        Update update =
                new Update()
                        .set("activo", cliente.activo())
                        .set("version", version)
                        .set("actualizadoEn", clock.instant());
        // Los eventos customer.* del backend no traen la fecha de nacimiento: no se borra la que hay.
        if (cliente.fechaNacimiento() != null) {
            update.set("fechaNacimiento", cliente.fechaNacimiento());
        }
        return upsertSiEsMasNuevo(cliente.clienteId(), version, update, ClienteRefDocument.class);
    }

    @Override
    public void completarFechaNacimiento(ClienteRef cliente) {
        if (cliente.fechaNacimiento() == null) {
            return;
        }
        mongo.updateFirst(
                Query.query(Criteria.where("_id").is(cliente.clienteId().toString()).and("fechaNacimiento").is(null)),
                new Update().set("fechaNacimiento", cliente.fechaNacimiento()).set("actualizadoEn", clock.instant()),
                ClienteRefDocument.class);
    }

    @Override
    public boolean guardarVehiculoSiEsMasNuevo(VehiculoRef vehiculo, long version) {
        Update update =
                new Update()
                        .set("clienteId", vehiculo.clienteId().toString())
                        .set("tipo", vehiculo.tipo())
                        .set("uso", vehiculo.uso())
                        .set("anioFabricacion", vehiculo.anioFabricacion())
                        .set("version", version)
                        .set("actualizadoEn", clock.instant());
        return upsertSiEsMasNuevo(vehiculo.vehiculoId(), version, update, VehiculoRefDocument.class);
    }

    private boolean upsertSiEsMasNuevo(UUID id, long version, Update update, Class<?> tipo) {
        Query query = Query.query(Criteria.where("_id").is(id.toString()).and("version").lt(version));
        try {
            mongo.upsert(query, update, tipo);
            return true;
        } catch (DuplicateKeyException versionIgualOMayor) {
            return false;
        }
    }
}
