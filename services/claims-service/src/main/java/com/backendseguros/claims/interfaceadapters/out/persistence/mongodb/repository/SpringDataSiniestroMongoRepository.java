package com.backendseguros.claims.interfaceadapters.out.persistence.mongodb.repository;

import com.backendseguros.claims.interfaceadapters.out.persistence.mongodb.document.SiniestroDocument;
import java.util.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataSiniestroMongoRepository
        extends MongoRepository<SiniestroDocument, String> {
    List<SiniestroDocument> findByPolizaId(String polizaId);
}
