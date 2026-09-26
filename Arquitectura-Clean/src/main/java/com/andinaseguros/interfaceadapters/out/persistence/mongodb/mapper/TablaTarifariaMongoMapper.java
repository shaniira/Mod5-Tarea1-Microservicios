package com.andinaseguros.interfaceadapters.out.persistence.mongodb.mapper;

import com.andinaseguros.entities.model.*;
import com.andinaseguros.entities.valueobject.*;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.document.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class TablaTarifariaMongoMapper {
    public TablaTarifariaDocument toDocument(TablaTarifaria x) {
        var d = new TablaTarifariaDocument();
        d.id = x.getId().toString();
        d.codigo = x.getCodigo();
        d.version = x.getVersion();
        d.tipoVehiculo = x.getTipoVehiculo();
        d.tipoUso = x.getTipoUso();
        d.primaBase = x.getPrimaBase().valor();
        d.primaMinima = x.getPrimaMinima().valor();
        d.inicioVigencia = x.getVigencia().inicio();
        d.finVigencia = x.getVigencia().fin();
        d.codigoNotaTecnica = x.getCodigoNotaTecnica();
        d.estado = x.getEstado();
        d.factores = x.getFactores().stream().map(this::factor).toList();
        return d;
    }

    private FactorRiesgoDocument factor(FactorRiesgo x) {
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

    public TablaTarifaria toDomain(TablaTarifariaDocument d) {
        var factores =
                d.factores.stream()
                        .map(
                                x ->
                                        new FactorRiesgo(
                                                UUID.fromString(x.id),
                                                x.codigo,
                                                x.nombre,
                                                x.tipoVariable,
                                                x.valorMinimo,
                                                x.valorMaximo,
                                                x.multiplicador,
                                                x.orden))
                        .toList();
        return new TablaTarifaria(
                UUID.fromString(d.id),
                d.codigo,
                d.version,
                d.tipoVehiculo,
                d.tipoUso,
                Dinero.soles(d.primaBase),
                Dinero.soles(d.primaMinima),
                new PeriodoVigencia(d.inicioVigencia, d.finVigencia),
                d.codigoNotaTecnica,
                d.estado,
                factores);
    }
}
