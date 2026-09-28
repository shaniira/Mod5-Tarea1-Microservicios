package com.backendseguros.quotation.interfaceadapters.out.persistence.mongodb.document;

import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Proyección customer_ref (paso 5.2): lo mínimo del cliente para tarificar. */
@Document("customer_ref")
public class ClienteRefDocument {
    @Id public String id;
    public LocalDate fechaNacimiento;
    public boolean activo;
    /** Versión del cliente (aggregateVersion); 0 si llegó por la lectura de refuerzo. */
    public long version;
    public Instant actualizadoEn;
}
