package com.backendseguros.claims.interfaceadapters.out.persistence.mongodb.document;

import com.backendseguros.claims.entities.enums.EstadoSiniestro;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Mismo formato que la colección siniestros del monolito, más la versión del agregado. */
@Document("siniestros")
public class SiniestroDocument {
    @Id public String id;
    @Indexed public String polizaId;
    public LocalDate fecha;
    public String tipo;
    public BigDecimal montoEstimado;
    public String moneda;
    public boolean responsabilidadAsegurado;
    public String gravedad;
    public EstadoSiniestro estado;

    // Los siniestros migrados desde el monolito no tienen este campo: se leen como versión 1.
    public Long version;
}
