package com.andinaseguros.customer.interfaceadapters.out.persistence.mongodb.document;

import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("clientes")
public class ClienteDocument {
    @Id public String id;
    public String tipoDocumento;

    @Indexed(unique = true)
    public String numeroDocumento;

    public String nombres;
    public String apellidos;
    public LocalDate fechaNacimiento;
    public String correo;
    public String telefono;
    public boolean activo;

    // Los clientes guardados antes de la fase 1 no tienen este campo: se leen como versión 1.
    public Long version;
}
