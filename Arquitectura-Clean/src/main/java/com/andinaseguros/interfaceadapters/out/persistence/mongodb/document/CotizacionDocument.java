package com.andinaseguros.interfaceadapters.out.persistence.mongodb.document;

import com.andinaseguros.entities.enums.EstadoCotizacion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("cotizaciones")
public class CotizacionDocument {
    @Id public String id;

    @Indexed(unique = true)
    public String numero;

    public String clienteId;
    public String vehiculoId;
    public String tablaTarifariaId;
    public BigDecimal prima;
    public String moneda;
    public LocalDateTime fechaCreacion;
    public LocalDateTime fechaExpiracion;
    public EstadoCotizacion estado;
    public String desgloseJson;
}
