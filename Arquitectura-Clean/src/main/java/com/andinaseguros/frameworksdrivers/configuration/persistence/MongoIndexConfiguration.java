package com.andinaseguros.frameworksdrivers.configuration.persistence;

import com.andinaseguros.interfaceadapters.out.persistence.mongodb.document.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
public class MongoIndexConfiguration implements ApplicationRunner {
    private final MongoTemplate mongo;

    public MongoIndexConfiguration(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public void run(ApplicationArguments args) {
        unique(ClienteDocument.class, "numeroDocumento");
        unique(VehiculoDocument.class, "placa");
        unique(CotizacionDocument.class, "numero");
        unique(PolizaDocument.class, "numero");
        unique(UsuarioDocument.class, "username");
        mongo.indexOps(UsuarioDocument.class)
                .ensureIndex(
                        new Index()
                                .on("email", Sort.Direction.ASC)
                                .named("email")
                                .unique()
                                .sparse());
        mongo.indexOps(UsuarioDocument.class)
                .ensureIndex(
                        new Index()
                                .on("googleSubject", Sort.Direction.ASC)
                                .named("googleSubject")
                                .unique()
                                .sparse());
    }

    private void unique(Class<?> type, String field) {
        mongo.indexOps(type)
                .ensureIndex(new Index().on(field, Sort.Direction.ASC).named(field).unique());
    }
}
