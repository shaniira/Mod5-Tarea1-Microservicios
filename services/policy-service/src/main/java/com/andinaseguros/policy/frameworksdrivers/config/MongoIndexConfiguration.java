package com.andinaseguros.policy.frameworksdrivers.config;

import com.andinaseguros.policy.interfaceadapters.out.persistence.mongodb.document.PolizaDocument;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

/**
 * Protección contra la doble emisión (paso 6.3): índice único por cotizacionId, parcial (solo las
 * pólizas que tienen cotización). Las pólizas renovadas no tienen cotización y no chocan entre sí.
 * Aunque dos solicitudes de la misma cotización lleguen a la vez a dos réplicas, MongoDB rechaza la
 * segunda.
 */
@Component
public class MongoIndexConfiguration implements ApplicationRunner {
    static final String INDICE_COTIZACION = "cotizacionId_unico";

    private final MongoTemplate mongo;

    public MongoIndexConfiguration(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void run(ApplicationArguments args) {
        mongo.indexOps(PolizaDocument.class)
                .ensureIndex(
                        new Index()
                                .on("cotizacionId", Sort.Direction.ASC)
                                .named(INDICE_COTIZACION)
                                .unique()
                                .partial(PartialIndexFilter.of(Criteria.where("cotizacionId").type(2))));
    }
}
