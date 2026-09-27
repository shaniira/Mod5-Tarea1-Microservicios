package com.andinaseguros.quotation.interfaceadapters.out.persistence.mongodb.repository;

import com.andinaseguros.quotation.entities.enums.*;
import com.andinaseguros.quotation.interfaceadapters.out.persistence.mongodb.document.TablaTarifariaDocument;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataTablaTarifariaMongoRepository
        extends MongoRepository<TablaTarifariaDocument, String> {
    List<TablaTarifariaDocument>
            findByTipoVehiculoAndTipoUsoAndEstadoAndInicioVigenciaLessThanEqualAndFinVigenciaGreaterThanEqualOrderByVersionDesc(
                    TipoVehiculo tipoVehiculo,
                    TipoUso tipoUso,
                    EstadoTablaTarifaria estado,
                    LocalDate inicio,
                    LocalDate fin);
}
