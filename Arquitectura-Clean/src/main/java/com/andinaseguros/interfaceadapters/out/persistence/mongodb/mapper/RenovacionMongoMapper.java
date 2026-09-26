package com.andinaseguros.interfaceadapters.out.persistence.mongodb.mapper;

import com.andinaseguros.entities.model.PropuestaRenovacion;
import com.andinaseguros.entities.valueobject.Dinero;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.document.RenovacionDocument;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RenovacionMongoMapper {
    private String id(UUID x) {
        return x == null ? null : x.toString();
    }

    private UUID uuid(String x) {
        return x == null ? null : UUID.fromString(x);
    }

    public RenovacionDocument toDocument(PropuestaRenovacion x) {
        var d = new RenovacionDocument();
        d.id = id(x.id());
        d.polizaOrigenId = id(x.polizaOrigenId());
        d.primaAnterior = x.primaAnterior().valor();
        d.nuevaPrima = x.nuevaPrima().valor();
        d.porcentajeVariacion = x.porcentajeVariacion();
        d.siniestrosConsiderados = x.siniestrosConsiderados();
        d.estado = x.estado();
        d.motivo = x.motivo();
        d.creadaEn = x.creadaEn();
        d.venceEn = x.venceEn();
        d.decididaEn = x.decididaEn();
        d.polizaRenovadaId = id(x.polizaRenovadaId());
        return d;
    }

    public PropuestaRenovacion toDomain(RenovacionDocument d) {
        return new PropuestaRenovacion(
                uuid(d.id),
                uuid(d.polizaOrigenId),
                Dinero.soles(d.primaAnterior),
                Dinero.soles(d.nuevaPrima),
                d.porcentajeVariacion,
                d.siniestrosConsiderados,
                d.estado,
                d.motivo,
                d.creadaEn,
                d.venceEn,
                d.decididaEn,
                uuid(d.polizaRenovadaId));
    }
}
