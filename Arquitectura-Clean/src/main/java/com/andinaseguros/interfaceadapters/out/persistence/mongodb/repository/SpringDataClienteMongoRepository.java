package com.andinaseguros.interfaceadapters.out.persistence.mongodb.repository;

import com.andinaseguros.interfaceadapters.out.persistence.mongodb.document.ClienteDocument;
import java.util.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataClienteMongoRepository extends MongoRepository<ClienteDocument, String> {
    Optional<ClienteDocument> findByNumeroDocumento(String documento);

    Optional<ClienteDocument> findByCorreo(String correo);
}
