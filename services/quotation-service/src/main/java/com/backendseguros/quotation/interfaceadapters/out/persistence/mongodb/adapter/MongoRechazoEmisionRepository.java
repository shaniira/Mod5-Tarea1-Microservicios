package com.backendseguros.quotation.interfaceadapters.out.persistence.mongodb.adapter;

import com.backendseguros.quotation.usecases.port.out.repository.RechazoEmisionRepository;
import java.time.Clock;
import java.util.UUID;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/** Colección rechazos_emision: un documento por evento (el _id es el eventId, idempotente). */
public class MongoRechazoEmisionRepository implements RechazoEmisionRepository {
    static final String COLECCION = "rechazos_emision";

    private final MongoTemplate mongo;
    private final Clock clock;

    public MongoRechazoEmisionRepository(MongoTemplate mongo, Clock clock) {
        this.mongo = mongo;
        this.clock = clock;
    }

    @Override
    public void registrar(UUID eventId, UUID cotizacionId, String codigo, String motivo) {
        mongo.upsert(
                Query.query(Criteria.where("_id").is(eventId.toString())),
                new Update()
                        .setOnInsert("cotizacionId", cotizacionId.toString())
                        .setOnInsert("codigo", codigo)
                        .setOnInsert("motivo", motivo)
                        .setOnInsert("registradoEn", clock.instant()),
                Document.class,
                COLECCION);
    }
}
