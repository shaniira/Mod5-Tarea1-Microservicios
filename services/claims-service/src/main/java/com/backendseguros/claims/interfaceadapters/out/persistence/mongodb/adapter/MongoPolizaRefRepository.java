package com.backendseguros.claims.interfaceadapters.out.persistence.mongodb.adapter;

import com.backendseguros.claims.entities.enums.EstadoPoliza;
import com.backendseguros.claims.entities.model.PolizaRef;
import com.backendseguros.claims.interfaceadapters.out.persistence.mongodb.document.PolizaRefDocument;
import com.backendseguros.claims.usecases.port.out.repository.PolizaRefRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class MongoPolizaRefRepository implements PolizaRefRepository {
    static final String ORIGEN_EVENTO = "POLICY_ISSUED";
    /** Cambio de estado publicado por policy-service (policy.renewed/expired/cancelled.v1). */
    static final String ORIGEN_POLICY_SERVICE = "POLICY_SERVICE";

    private final MongoTemplate mongo;
    private final Clock clock;

    public MongoPolizaRefRepository(MongoTemplate mongo, Clock clock) {
        this.mongo = mongo;
        this.clock = clock;
    }

    @Override
    public Optional<PolizaRef> buscarPorId(UUID polizaId) {
        return Optional.ofNullable(mongo.findById(polizaId.toString(), PolizaRefDocument.class))
                .map(
                        d ->
                                new PolizaRef(
                                        UUID.fromString(d.id),
                                        d.clienteId == null ? null : UUID.fromString(d.clienteId),
                                        d.numero,
                                        d.estado));
    }

    /** Upsert atómico con $setOnInsert: si la póliza ya estaba, no se toca ningún campo. */
    @Override
    public boolean registrarEmitidaSiNoExiste(PolizaRef poliza) {
        Update update =
                new Update()
                        .setOnInsert("clienteId", poliza.clienteId() == null ? null : poliza.clienteId().toString())
                        .setOnInsert("numero", poliza.numero())
                        .setOnInsert("estado", poliza.estado())
                        .setOnInsert("origen", ORIGEN_EVENTO)
                        .setOnInsert("actualizadoEn", clock.instant());
        var resultado =
                mongo.upsert(
                        Query.query(Criteria.where("_id").is(poliza.polizaId().toString())),
                        update,
                        PolizaRefDocument.class);
        return resultado.getUpsertedId() != null;
    }

    @Override
    public boolean actualizarEstadoSiEsMasNuevo(UUID polizaId, EstadoPoliza estado, long version) {
        Query query =
                Query.query(
                        Criteria.where("_id")
                                .is(polizaId.toString())
                                .orOperator(
                                        Criteria.where("version").lt(version),
                                        Criteria.where("version").exists(false)));
        Update update =
                new Update()
                        .set("estado", estado)
                        .set("version", version)
                        .set("origen", ORIGEN_POLICY_SERVICE)
                        .set("actualizadoEn", clock.instant());
        return mongo.updateFirst(query, update, PolizaRefDocument.class).getModifiedCount() > 0;
    }
}
