package com.andinaseguros.interfaceadapters.out.persistence.mongodb.repository;

import com.andinaseguros.interfaceadapters.out.persistence.mongodb.document.FactorRiesgoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataFactorRiesgoMongoRepository
        extends MongoRepository<FactorRiesgoDocument, String> {}
