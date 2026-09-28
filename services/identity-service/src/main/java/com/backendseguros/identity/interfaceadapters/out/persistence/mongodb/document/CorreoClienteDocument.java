package com.backendseguros.identity.interfaceadapters.out.persistence.mongodb.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** _id = customerId; el correo se guarda en minúsculas y se busca por él. */
@Document("customer_email_index")
public class CorreoClienteDocument {
    @Id public String id;

    @Indexed(sparse = true)
    public String correo;

    public long version;
    public Instant actualizadoEn;
}
