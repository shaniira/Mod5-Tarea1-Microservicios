package com.andinaseguros.interfaceadapters.out.persistence.mongodb.document;

import com.andinaseguros.entities.enums.EstadoPoliza;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("polizas")
public class PolizaDocument {
    @Id public String id;

    @Indexed(unique = true)
    public String numero;

    @Indexed(unique = true)
    public String cotizacionId;

    public String clienteId;
    public String vehiculoId;
    public BigDecimal prima;
    public String moneda;
    public LocalDate inicioVigencia;
    public LocalDate finVigencia;

    @Indexed(unique = true, sparse = true)
    public String renovacionOrigenId;

    public EstadoPoliza estado;
}
