package com.andinaseguros.policy.interfaceadapters.out.persistence.mongodb.adapter;

import com.andinaseguros.policy.entities.model.CotizacionAceptada;
import com.andinaseguros.policy.entities.model.SiniestroRef;
import com.andinaseguros.policy.entities.valueobject.Dinero;
import com.andinaseguros.policy.interfaceadapters.out.persistence.mongodb.document.CotizacionAceptadaDocument;
import com.andinaseguros.policy.interfaceadapters.out.persistence.mongodb.document.SiniestroRefDocument;
import com.andinaseguros.policy.usecases.port.out.repository.CotizacionesAceptadasRepository;
import com.andinaseguros.policy.usecases.port.out.repository.SiniestrosRefRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/** Proyecciones accepted_quotes y claim_ref (paso 6.2). */
public class MongoProyeccionesRepository implements CotizacionesAceptadasRepository, SiniestrosRefRepository {
    static final String ORIGEN_EVENTO = "QUOTE_ACCEPTED";
    /** Documento que deja la carga inicial de claim_ref (script de migración). */
    static final String ESTADO_SINCRONIZACION = "sync_state";
    static final String CLAIM_REF_CARGADA = "claim_ref";

    private final MongoTemplate mongo;
    private final Clock clock;

    public MongoProyeccionesRepository(MongoTemplate mongo, Clock clock) {
        this.mongo = mongo;
        this.clock = clock;
    }

    @Override
    public Optional<CotizacionAceptada> buscar(UUID cotizacionId) {
        return Optional.ofNullable(mongo.findById(cotizacionId.toString(), CotizacionAceptadaDocument.class))
                .map(
                        d ->
                                new CotizacionAceptada(
                                        UUID.fromString(d.id),
                                        d.numero,
                                        UUID.fromString(d.clienteId),
                                        UUID.fromString(d.vehiculoId),
                                        new Dinero(d.prima, d.moneda),
                                        d.expira));
    }

    /** $setOnInsert: una cotización aceptada no cambia; un evento repetido no toca nada. */
    @Override
    public boolean registrarSiNoExiste(CotizacionAceptada c) {
        Update update =
                new Update()
                        .setOnInsert("numero", c.numero())
                        .setOnInsert("clienteId", c.clienteId().toString())
                        .setOnInsert("vehiculoId", c.vehiculoId().toString())
                        .setOnInsert("prima", c.prima().valor())
                        .setOnInsert("moneda", c.prima().moneda())
                        .setOnInsert("expira", c.expira())
                        .setOnInsert("origen", ORIGEN_EVENTO)
                        .setOnInsert("recibidaEn", clock.instant());
        var resultado =
                mongo.upsert(
                        Query.query(Criteria.where("_id").is(c.cotizacionId().toString())),
                        update,
                        CotizacionAceptadaDocument.class);
        return resultado.getUpsertedId() != null;
    }

    @Override
    public List<SiniestroRef> listarPorPoliza(UUID polizaId) {
        return mongo.find(Query.query(Criteria.where("polizaId").is(polizaId.toString())), SiniestroRefDocument.class)
                .stream()
                .map(d -> new SiniestroRef(UUID.fromString(d.id), UUID.fromString(d.polizaId), d.abierto, d.responsabilidadAsegurado))
                .toList();
    }

    /** Upsert condicional: solo si no existe o su versión es menor (mismo patrón que identity). */
    @Override
    public boolean guardarSiEsMasNuevo(SiniestroRef s, long version) {
        Query query =
                Query.query(Criteria.where("_id").is(s.siniestroId().toString()).and("version").lt(version));
        Update update =
                new Update()
                        .set("polizaId", s.polizaId().toString())
                        .set("abierto", s.abierto())
                        .set("responsabilidadAsegurado", s.responsabilidadAsegurado())
                        .set("version", version)
                        .set("actualizadoEn", clock.instant());
        try {
            mongo.upsert(query, update, SiniestroRefDocument.class);
            return true;
        } catch (DuplicateKeyException versionIgualOMayor) {
            return false;
        }
    }

    /** true si la carga inicial de claim_ref ya se hizo (la marca la deja migrar-polizas.sh). */
    public boolean cargaInicialHecha() {
        return mongo.getCollection(ESTADO_SINCRONIZACION)
                        .countDocuments(new Document("_id", CLAIM_REF_CARGADA))
                > 0;
    }
}
