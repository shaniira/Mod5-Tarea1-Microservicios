package com.andinaseguros.customer.interfaceadapters.out.persistence.mongodb.mapper;

import com.andinaseguros.customer.entities.model.Cliente;
import com.andinaseguros.customer.interfaceadapters.out.persistence.mongodb.document.ClienteDocument;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ClienteMongoMapper {
    public ClienteDocument toDocument(Cliente x) {
        var d = new ClienteDocument();
        d.id = x.getId().toString();
        d.tipoDocumento = x.getTipoDocumento();
        d.numeroDocumento = x.getNumeroDocumento();
        d.nombres = x.getNombres();
        d.apellidos = x.getApellidos();
        d.fechaNacimiento = x.getFechaNacimiento();
        d.correo = x.getCorreo();
        d.telefono = x.getTelefono();
        d.activo = x.isActivo();
        d.version = x.getVersion();
        return d;
    }

    public Cliente toDomain(ClienteDocument d) {
        return new Cliente(
                UUID.fromString(d.id),
                d.tipoDocumento,
                d.numeroDocumento,
                d.nombres,
                d.apellidos,
                d.fechaNacimiento,
                d.correo,
                d.telefono,
                d.activo,
                d.version == null ? 1 : d.version);
    }
}
