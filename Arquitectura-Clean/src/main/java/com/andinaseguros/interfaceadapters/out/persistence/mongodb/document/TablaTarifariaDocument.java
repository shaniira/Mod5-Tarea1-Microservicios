package com.andinaseguros.interfaceadapters.out.persistence.mongodb.document;

import com.andinaseguros.entities.enums.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("tablas_tarifarias")
@CompoundIndex(name = "codigo_version_unique", def = "{'codigo':1,'version':1}", unique = true)
public class TablaTarifariaDocument {
    @Id public String id;
    public String codigo;
    public int version;
    public TipoVehiculo tipoVehiculo;
    public TipoUso tipoUso;
    public BigDecimal primaBase;
    public BigDecimal primaMinima;
    public LocalDate inicioVigencia;
    public LocalDate finVigencia;
    public String codigoNotaTecnica;
    public EstadoTablaTarifaria estado;
    public List<FactorRiesgoDocument> factores = new ArrayList<>();
}
