package com.andinaseguros.interfaceadapters.out.persistence.mongodb.document;

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
}
