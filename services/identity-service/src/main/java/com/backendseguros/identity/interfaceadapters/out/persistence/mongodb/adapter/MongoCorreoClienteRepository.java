package com.backendseguros.identity.interfaceadapters.out.persistence.mongodb.adapter;

import com.backendseguros.identity.entities.model.CorreoCliente;
import com.backendseguros.identity.interfaceadapters.out.persistence.mongodb.document.CorreoClienteDocument;
import com.backendseguros.identity.usecases.port.out.repository.CorreoClienteRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class MongoCorreoClienteRepository implements CorreoClienteRepository {
    private final MongoTemplate mongo;
    private final Clock clock;

    public MongoCorreoClienteRepository(MongoTemplate mongo, Clock clock) {
        this.mongo = mongo;
        this.clock = clock;
    }

    @Override
    public Optional<UUID> buscarClientePorCorreo(String correo) {
        String normalizado = CorreoCliente.normalizar(correo);
        if (normalizado == null) {
            return Optional.empty();
        }
        // Si por datos de origen hubiera dos clientes con el mismo correo, gana el más reciente.
        Query query =
                Query.query(Criteria.where("correo").is(normalizado))
                        .with(Sort.by(Sort.Direction.DESC, "actualizadoEn"))
                        .limit(1);
        return Optional.ofNullable(mongo.findOne(query, CorreoClienteDocument.class))
                .map(d -> UUID.fromString(d.id));
    }

    /**
     * Upsert condicional y atómico: solo coincide si no existe o tiene versión menor. Si ya hay una
     * versión igual o mayor, el upsert choca con el _id y MongoDB lo rechaza.
     */
    @Override
    public boolean guardarSiEsMasNuevo(CorreoCliente entrada) {
        Query query =
                Query.query(
                        Criteria.where("_id")
                                .is(entrada.clienteId().toString())
                                .and("version")
                                .lt(entrada.version()));
        Update update =
                new Update()
                        .set("correo", entrada.correo())
                        .set("version", entrada.version())
                        .set("actualizadoEn", clock.instant());
        try {
            mongo.upsert(query, update, CorreoClienteDocument.class);
            return true;
        } catch (DuplicateKeyException versionIgualOMayor) {
            return false;
        }
    }
}
