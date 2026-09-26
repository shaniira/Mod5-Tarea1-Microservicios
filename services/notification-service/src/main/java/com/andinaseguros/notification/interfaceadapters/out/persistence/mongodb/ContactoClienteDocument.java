package com.andinaseguros.notification.interfaceadapters.out.persistence.mongodb;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("customer_contacts")
public class ContactoClienteDocument {
    @Id public String id;
    public String nombre;
    public String correo;
    public String telefono;
    public long version;
    public Instant actualizadoEn;
}
