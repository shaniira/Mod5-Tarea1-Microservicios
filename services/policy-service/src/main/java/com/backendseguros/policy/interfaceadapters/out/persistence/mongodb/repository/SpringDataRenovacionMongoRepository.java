package com.backendseguros.policy.interfaceadapters.out.persistence.mongodb.repository;

import com.backendseguros.policy.interfaceadapters.out.persistence.mongodb.document.RenovacionDocument;
import java.util.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataRenovacionMongoRepository
        extends MongoRepository<RenovacionDocument, String> {
    List<RenovacionDocument> findByPolizaOrigenIdOrderByCreadaEnDesc(String polizaId);
}
