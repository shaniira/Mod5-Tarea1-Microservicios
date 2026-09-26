package com.andinaseguros.interfaceadapters.out.persistence.mongodb.mapper;

import com.andinaseguros.entities.model.Vehiculo;
import com.andinaseguros.entities.valueobject.Placa;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.document.VehiculoDocument;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class VehiculoMongoMapper {
    public VehiculoDocument toDocument(Vehiculo x) {
        var d = new VehiculoDocument();
        d.id = x.getId().toString();
        d.clienteId = x.getClienteId().toString();
        d.placa = x.getPlaca().valor();
        d.marca = x.getMarca();
        d.modelo = x.getModelo();
        d.anioFabricacion = x.getAnioFabricacion();
        d.tipo = x.getTipo();
        d.uso = x.getUso();
        d.zonaCirculacion = x.getZonaCirculacion();
        return d;
    }

    public Vehiculo toDomain(VehiculoDocument d) {
        return new Vehiculo(
                UUID.fromString(d.id),
                UUID.fromString(d.clienteId),
                new Placa(d.placa),
                d.marca,
                d.modelo,
                d.anioFabricacion,
                d.tipo,
                d.uso,
                d.zonaCirculacion);
    }
}
