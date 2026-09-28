package com.andinaseguros.quotation.interfaceadapters.out.event;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Turno del relay del Outbox cuando hay varias réplicas (fase 7, criterio "2 réplicas de cada
 * servicio"). Solo la réplica que tiene el turno publica; así dos réplicas no envían el mismo
 * evento a la vez ni lo adelantan fuera de orden.
 *
 * <p>El turno es un documento en {@code outbox_lock} con dueño y vencimiento. Cada pasada lo
 * renueva; si la réplica dueña muere, otra lo toma cuando vence ({@code duracion}). Es una
 * operación atómica de MongoDB: la condición "vencido o mío" y la escritura van en el mismo
 * findAndModify; si otra réplica lo tiene vigente, el upsert choca con el _id y no hay turno.
 */
public class MongoOutboxLease implements BooleanSupplier {
    private static final Logger log = LoggerFactory.getLogger(MongoOutboxLease.class);
    static final String COLECCION = "outbox_lock";
    private static final String ID = "relay";

    private final MongoTemplate mongo;
    private final String instancia;
    private final Duration duracion;
    private final Clock clock;
    private boolean teniaTurno;

    public MongoOutboxLease(MongoTemplate mongo, String instancia, Duration duracion, Clock clock) {
        this.mongo = mongo;
        this.instancia = instancia;
        this.duracion = duracion;
        this.clock = clock;
    }

    /** true si esta réplica tiene (o acaba de tomar) el turno. */
    @Override
    public boolean getAsBoolean() {
        Instant ahora = clock.instant();
        Query query =
                Query.query(
                        Criteria.where("_id")
                                .is(ID)
                                .orOperator(
                                        Criteria.where("owner").is(instancia),
                                        Criteria.where("lockedUntil").lt(ahora)));
        Update update = new Update().set("owner", instancia).set("lockedUntil", ahora.plus(duracion));
        boolean turno;
        try {
            mongo.findAndModify(
                    query, update, FindAndModifyOptions.options().upsert(true), Document.class, COLECCION);
            turno = true;
        } catch (DuplicateKeyException otraReplicaLoTiene) {
            turno = false;
        }
        if (turno != teniaTurno) {
            log.info("Relay del Outbox: la réplica {} {} el turno", instancia, turno ? "toma" : "cede");
            teniaTurno = turno;
        }
        return turno;
    }
}
