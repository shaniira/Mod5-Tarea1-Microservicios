package com.andinaseguros.policy.interfaceadapters.out.persistence.mongodb.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Proyección accepted_quotes (paso 6.2), alimentada por quote.accepted.v1. */
@Document("accepted_quotes")
public class CotizacionAceptadaDocument {
    @Id public String id;
    public String numero;
    public String clienteId;
    public String vehiculoId;
    public BigDecimal prima;
    public String moneda;
    public LocalDateTime expira;
    /** QUOTE_ACCEPTED (evento) o CARGA_INICIAL (script de migración). */
    public String origen;
    public Instant recibidaEn;
}
