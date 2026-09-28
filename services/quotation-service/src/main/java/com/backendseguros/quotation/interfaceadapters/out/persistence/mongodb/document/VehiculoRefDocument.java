package com.backendseguros.quotation.interfaceadapters.out.persistence.mongodb.document;

import com.backendseguros.quotation.entities.enums.TipoUso;
import com.backendseguros.quotation.entities.enums.TipoVehiculo;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Proyección vehicle_ref (paso 5.2): dueño, tipo, uso y año del vehículo. */
@Document("vehicle_ref")
public class VehiculoRefDocument {
    @Id public String id;
    public String clienteId;
    public TipoVehiculo tipo;
    public TipoUso uso;
    public int anioFabricacion;
    public long version;
    public Instant actualizadoEn;
}
