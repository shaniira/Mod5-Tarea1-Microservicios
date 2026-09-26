package com.andinaseguros.interfaceadapters.out.persistence.mongodb.mapper;

import com.andinaseguros.entities.model.Poliza;
import com.andinaseguros.entities.valueobject.*;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.document.PolizaDocument;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PolizaMongoMapper {
    private String id(UUID x) {
        return x == null ? null : x.toString();
    }

    private UUID uuid(String x) {
        return x == null ? null : UUID.fromString(x);
    }

    public PolizaDocument toDocument(Poliza x) {
        var d = new PolizaDocument();
        d.id = id(x.getId());
        d.numero = x.getNumero();
        d.cotizacionId = id(x.getCotizacionId());
        d.clienteId = id(x.getClienteId());
        d.vehiculoId = id(x.getVehiculoId());
        d.prima = x.getPrima().valor();
        d.moneda = x.getPrima().moneda();
        d.inicioVigencia = x.getVigencia().inicio();
        d.finVigencia = x.getVigencia().fin();
        d.renovacionOrigenId = id(x.getRenovacionOrigenId());
        d.estado = x.getEstado();
        return d;
    }

    public Poliza toDomain(PolizaDocument d) {
        return new Poliza(
                uuid(d.id),
                d.numero,
                uuid(d.cotizacionId),
                uuid(d.clienteId),
                uuid(d.vehiculoId),
                new Dinero(d.prima, d.moneda),
                new PeriodoVigencia(d.inicioVigencia, d.finVigencia),
                d.estado,
                uuid(d.renovacionOrigenId));
    }
}
