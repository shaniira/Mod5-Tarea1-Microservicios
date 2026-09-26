package com.andinaseguros.interfaceadapters.out.persistence.mongodb.document;

import com.andinaseguros.entities.enums.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("vehiculos")
public class VehiculoDocument {
    @Id public String id;
    public String clienteId;

    @Indexed(unique = true)
    public String placa;

    public String marca;
    public String modelo;
    public int anioFabricacion;
    public TipoVehiculo tipo;
    public TipoUso uso;
    public String zonaCirculacion;
}
