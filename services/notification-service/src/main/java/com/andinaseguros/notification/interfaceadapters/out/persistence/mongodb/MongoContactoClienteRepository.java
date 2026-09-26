package com.andinaseguros.notification.interfaceadapters.out.persistence.mongodb;

import com.andinaseguros.notification.entities.model.ContactoCliente;
import com.andinaseguros.notification.usecases.port.out.ContactoClienteRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class MongoContactoClienteRepository implements ContactoClienteRepository {
    private final MongoTemplate mongo;
    private final Clock clock;

    public MongoContactoClienteRepository(MongoTemplate mongo, Clock clock) {
        this.mongo = mongo;
        this.clock = clock;
    }

    @Override
    public Optional<ContactoCliente> buscar(UUID clienteId) {
        return Optional.ofNullable(mongo.findById(clienteId.toString(), ContactoClienteDocument.class))
                .map(
                        d ->
                                new ContactoCliente(
                                        UUID.fromString(d.id),
                                        d.nombre,
                                        d.correo,
                                        d.telefono,
                                        d.version));
    }

    /**
     * Upsert condicional y atómico: solo coincide si el documento no existe o tiene una versión
     * menor. Si existe con versión igual o mayor, el upsert intenta insertar un _id repetido y
     * MongoDB lo rechaza, lo que significa "ya tengo algo igual o más nuevo".
     */
    @Override
    public boolean guardarSiEsMasNuevo(ContactoCliente contacto) {
        Query query =
                Query.query(
                        Criteria.where("_id")
                                .is(contacto.clienteId().toString())
                                .and("version")
                                .lt(contacto.version()));
        Update update =
                new Update()
                        .set("nombre", contacto.nombre())
                        .set("correo", contacto.correo())
                        .set("telefono", contacto.telefono())
                        .set("version", contacto.version())
                        .set("actualizadoEn", clock.instant());
        try {
            mongo.upsert(query, update, ContactoClienteDocument.class);
            return true;
        } catch (DuplicateKeyException versionIgualOMayor) {
            return false;
        }
    }
}
