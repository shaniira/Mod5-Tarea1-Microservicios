package com.andinaseguros.interfaceadapters.out.persistence.mongodb.mapper;

import com.andinaseguros.entities.model.FactorRiesgo;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.document.FactorRiesgoDocument;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FactorRiesgoMongoMapper {
    public FactorRiesgoDocument toDocument(FactorRiesgo x) {
        var d = new FactorRiesgoDocument();
        d.id = x.id().toString();
        d.codigo = x.codigo();
        d.nombre = x.nombre();
        d.tipoVariable = x.tipoVariable();
        d.valorMinimo = x.valorMinimo();
        d.valorMaximo = x.valorMaximo();
        d.multiplicador = x.multiplicador();
        d.orden = x.orden();
        return d;
    }

    public FactorRiesgo toDomain(FactorRiesgoDocument d) {
        return new FactorRiesgo(
                UUID.fromString(d.id),
                d.codigo,
                d.nombre,
                d.tipoVariable,
                d.valorMinimo,
                d.valorMaximo,
                d.multiplicador,
                d.orden);
    }
}
