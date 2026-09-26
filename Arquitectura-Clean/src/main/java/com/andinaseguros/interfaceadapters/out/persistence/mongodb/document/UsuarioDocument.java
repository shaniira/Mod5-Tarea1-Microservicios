package com.andinaseguros.interfaceadapters.out.persistence.mongodb.document;

import com.andinaseguros.entities.enums.RolUsuario;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;

@Document("usuarios")
@CompoundIndex(name = "provider_identity", def = "{'provider': 1, 'providerUserId': 1}", unique = true, sparse = true)
public class UsuarioDocument {
    @Id public String id;

    @Indexed(unique = true)
    public String username;

    @Indexed(unique = true, sparse = true)
    public String email;

    public String nombres;
    public String apellidos;

    public String passwordHash;

    @Indexed(unique = true, sparse = true)
    public String googleSubject;

    public RolUsuario rol;
    public boolean activo;
    public String mfaSecret;
    public boolean mfaHabilitado;
    public String provider;
    public String providerUserId;
    public String facebookAccessToken;
    public long facebookAccessTokenExpiresAt;
    public String facebookScopes;
}
