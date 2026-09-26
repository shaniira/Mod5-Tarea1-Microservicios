package com.andinaseguros.interfaceadapters.out.persistence.mongodb.mapper;

import com.andinaseguros.entities.model.Usuario;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.document.UsuarioDocument;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMongoMapper {
    public UsuarioDocument toDocument(Usuario x) {
        var d = new UsuarioDocument();
        d.id = x.getId().toString();
        d.username = x.getUsername();
        d.email = x.getEmail();
        d.nombres = x.getNombres();
        d.apellidos = x.getApellidos();
        d.passwordHash = x.getPasswordHash();
        d.googleSubject = x.getGoogleSubject();
        d.rol = x.getRol();
        d.activo = x.isActivo();
        d.mfaSecret = x.getMfaSecret();
        d.mfaHabilitado = x.isMfaHabilitado();
        d.provider = x.getProvider();
        d.providerUserId = x.getProviderUserId();
        d.facebookAccessToken = x.getFacebookAccessToken();
        d.facebookAccessTokenExpiresAt = x.getFacebookAccessTokenExpiresAt();
        d.facebookScopes = x.getFacebookScopes();
        return d;
    }

    public Usuario toDomain(UsuarioDocument d) {
        return new Usuario(
                UUID.fromString(d.id),
                d.username,
                d.email,
                d.nombres,
                d.apellidos,
                d.passwordHash,
                d.googleSubject,
                d.rol,
                d.activo,
                d.mfaSecret,
                d.mfaHabilitado,
                d.provider,
                d.providerUserId,
                d.facebookAccessToken,
                d.facebookAccessTokenExpiresAt,
                d.facebookScopes);
    }
}
