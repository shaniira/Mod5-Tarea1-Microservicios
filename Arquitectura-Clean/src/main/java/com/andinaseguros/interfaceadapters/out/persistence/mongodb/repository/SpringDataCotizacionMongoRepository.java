package com.andinaseguros.interfaceadapters.out.persistence.mongodb.repository;

import com.andinaseguros.entities.enums.EstadoCotizacion;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.document.CotizacionDocument;
import java.util.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataCotizacionMongoRepository
        extends MongoRepository<CotizacionDocument, String> {
    List<CotizacionDocument> findByClienteId(String clienteId);

    List<CotizacionDocument> findByEstadoOrderByFechaCreacionDesc(EstadoCotizacion estado);
}
