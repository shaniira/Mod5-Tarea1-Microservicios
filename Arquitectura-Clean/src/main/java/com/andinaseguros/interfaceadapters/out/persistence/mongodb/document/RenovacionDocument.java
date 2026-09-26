package com.andinaseguros.interfaceadapters.out.persistence.mongodb.document;

import com.andinaseguros.entities.enums.EstadoRenovacion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("propuestas_renovacion")
public class RenovacionDocument {
    @Id public String id;
    @Indexed public String polizaOrigenId;
    public BigDecimal primaAnterior;
    public BigDecimal nuevaPrima;
    public BigDecimal porcentajeVariacion;
    public int siniestrosConsiderados;
    public EstadoRenovacion estado;
    public String motivo;
    public LocalDateTime creadaEn;
    public LocalDateTime venceEn;
    public LocalDateTime decididaEn;
    public String polizaRenovadaId;
}
