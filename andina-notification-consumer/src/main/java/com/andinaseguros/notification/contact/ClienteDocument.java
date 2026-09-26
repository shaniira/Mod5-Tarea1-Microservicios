package com.andinaseguros.notification.contact;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("clientes")
public class ClienteDocument {
    @Id public String id;
    public String nombres;
    public String apellidos;
    public String correo;
    public String telefono;
}
