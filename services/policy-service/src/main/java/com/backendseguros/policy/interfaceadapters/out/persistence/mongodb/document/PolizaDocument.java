package com.backendseguros.policy.interfaceadapters.out.persistence.mongodb.document;

import com.backendseguros.policy.entities.enums.EstadoPoliza;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Mismo formato que la colección polizas del monolito, más la versión. El índice único de
 * cotizacionId (paso 6.3) se crea aparte (MongoIndexConfiguration) como índice parcial: las pólizas
 * renovadas no tienen cotización y no deben chocar entre sí.
 */
@Document("polizas")
public class PolizaDocument {
    @Id public String id;

    @Indexed(unique = true)
    public String numero;

    public String cotizacionId;
    @Indexed public String clienteId;
    public String vehiculoId;
    public BigDecimal prima;
    public String moneda;
    public LocalDate inicioVigencia;
    public LocalDate finVigencia;

    @Indexed(unique = true, sparse = true)
    public String renovacionOrigenId;

    public EstadoPoliza estado;

    // Las pólizas migradas desde el monolito no tienen este campo: se leen como versión 1.
    public Long version;
}
