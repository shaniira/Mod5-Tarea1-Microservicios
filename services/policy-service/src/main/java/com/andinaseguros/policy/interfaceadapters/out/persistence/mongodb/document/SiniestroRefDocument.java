package com.andinaseguros.policy.interfaceadapters.out.persistence.mongodb.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Proyección claim_ref (paso 6.2): un documento por siniestro, alimentado por claim.*. */
@Document("claim_ref")
public class SiniestroRefDocument {
    @Id public String id;
    @Indexed public String polizaId;
    public boolean abierto;
    public boolean responsabilidadAsegurado;
    public long version;
    public Instant actualizadoEn;
}
