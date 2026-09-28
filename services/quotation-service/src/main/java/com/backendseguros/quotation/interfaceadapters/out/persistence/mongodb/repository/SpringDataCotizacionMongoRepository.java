package com.backendseguros.quotation.interfaceadapters.out.persistence.mongodb.repository;

import com.backendseguros.quotation.entities.enums.EstadoCotizacion;
import com.backendseguros.quotation.interfaceadapters.out.persistence.mongodb.document.CotizacionDocument;
import java.util.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataCotizacionMongoRepository
        extends MongoRepository<CotizacionDocument, String> {
    List<CotizacionDocument> findByClienteId(String clienteId);

    List<CotizacionDocument> findByEstadoOrderByFechaCreacionDesc(EstadoCotizacion estado);
}
