package com.andinaseguros.interfaceadapters.out.persistence.mongodb.mapper;

import com.andinaseguros.entities.model.*;
import com.andinaseguros.entities.valueobject.Dinero;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.document.CotizacionDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CotizacionMongoMapper {
    private final ObjectMapper json;

    public CotizacionMongoMapper(ObjectMapper json) {
        this.json = json;
    }

    public CotizacionDocument toDocument(Cotizacion x) {
        var d = new CotizacionDocument();
        d.id = x.getId().toString();
        d.numero = x.getNumero();
        d.clienteId = x.getClienteId().toString();
        d.vehiculoId = x.getVehiculoId().toString();
        d.tablaTarifariaId = x.getTablaTarifariaId().toString();
        d.prima = x.getPrima().valor();
        d.moneda = x.getPrima().moneda();
        d.fechaCreacion = x.getFechaCreacion();
        d.fechaExpiracion = x.getFechaExpiracion();
        d.estado = x.getEstado();
        d.desgloseJson = write(x.getDesglose());
        return d;
    }

    public Cotizacion toDomain(CotizacionDocument d) {
        return new Cotizacion(
                UUID.fromString(d.id),
                d.numero,
                UUID.fromString(d.clienteId),
                UUID.fromString(d.vehiculoId),
                UUID.fromString(d.tablaTarifariaId),
                new Dinero(d.prima, d.moneda),
                d.fechaCreacion,
                d.fechaExpiracion,
                d.estado,
                read(d.desgloseJson));
    }

    private String write(ResultadoTarificacion x) {
        if (x == null) return null;
        try {
            return json.writeValueAsString(x);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo guardar el desglose", e);
        }
    }

    private ResultadoTarificacion read(String x) {
        if (x == null || x.isBlank()) return null;
        try {
            return json.readValue(x, ResultadoTarificacion.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo leer el desglose", e);
        }
    }
}
