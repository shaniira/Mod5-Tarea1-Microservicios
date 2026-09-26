package com.andinaseguros.interfaceadapters.out.persistence.mongodb.repository;

import com.andinaseguros.interfaceadapters.out.persistence.mongodb.document.VehiculoDocument;
import java.util.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataVehiculoMongoRepository
        extends MongoRepository<VehiculoDocument, String> {
    Optional<VehiculoDocument> findByPlaca(String placa);

    List<VehiculoDocument> findByClienteId(String clienteId);
}
