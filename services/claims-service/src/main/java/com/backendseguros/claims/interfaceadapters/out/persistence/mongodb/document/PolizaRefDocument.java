package com.backendseguros.claims.interfaceadapters.out.persistence.mongodb.document;

import com.backendseguros.claims.entities.enums.EstadoPoliza;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Proyección policy_ref (paso 4.3): copia mínima de cada póliza. */
@Document("policy_ref")
public class PolizaRefDocument {
    @Id public String id;
    public String clienteId;
    public String numero;
    public EstadoPoliza estado;
    /** De dónde salió el último valor: POLICY_ISSUED, POLICY_SERVICE (eventos) o CARGA_INICIAL (script). */
    public String origen;
    /** Versión de la póliza según policy-service; no existe en la carga inicial ni en eventos del backend. */
    public Long version;
    public Instant actualizadoEn;
}
