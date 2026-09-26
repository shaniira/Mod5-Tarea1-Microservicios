package com.andinaseguros.interfaceadapters.out.persistence.mongodb.repository;

import com.andinaseguros.interfaceadapters.out.persistence.mongodb.document.PolizaDocument;
import java.util.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataPolizaMongoRepository extends MongoRepository<PolizaDocument, String> {
    Optional<PolizaDocument> findByNumero(String numero);

    Optional<PolizaDocument> findByCotizacionId(String cotizacionId);

    List<PolizaDocument> findByClienteId(String clienteId);
}
