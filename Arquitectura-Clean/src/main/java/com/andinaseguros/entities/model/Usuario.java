package com.andinaseguros.entities.model;

import com.andinaseguros.entities.enums.RolUsuario;
import java.util.UUID;

public class Usuario {
    private final UUID id;
    private final String username;
    private final String email;
    private final String nombres;
    private final String apellidos;
    private final String passwordHash;
    private final String googleSubject;
    private final RolUsuario rol;
    private final boolean activo;
    private final String mfaSecret;
    private final boolean mfaHabilitado;
    private final String provider;
    private final String providerUserId;
    private final String facebookAccessToken;
    private final long facebookAccessTokenExpiresAt;
    private final String facebookScopes;

    public Usuario(
            UUID id,
            String username,
            String email,
            String passwordHash,
            String googleSubject,
            RolUsuario rol,
            boolean activo,
            String mfaSecret,
            boolean mfaHabilitado) {
        this(
                id, username, email, null, null, passwordHash, googleSubject, rol, activo,
                mfaSecret, mfaHabilitado, null, null, null, 0, null);
    }

    public Usuario(
            UUID id,
            String username,
            String email,
            String passwordHash,
            String googleSubject,
            RolUsuario rol,
            boolean activo,
            String mfaSecret,
            boolean mfaHabilitado,
            String provider,
            String providerUserId,
            String facebookAccessToken,
            long facebookAccessTokenExpiresAt,
            String facebookScopes) {
        this(
                id, username, email, null, null, passwordHash, googleSubject, rol, activo,
                mfaSecret, mfaHabilitado, provider, providerUserId, facebookAccessToken,
                facebookAccessTokenExpiresAt, facebookScopes);
    }

    public Usuario(
            UUID id,
            String username,
            String email,
            String nombres,
            String apellidos,
            String passwordHash,
            String googleSubject,
            RolUsuario rol,
            boolean activo,
            String mfaSecret,
            boolean mfaHabilitado,
            String provider,
            String providerUserId,
            String facebookAccessToken,
            long facebookAccessTokenExpiresAt,
            String facebookScopes) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.passwordHash = passwordHash;
        this.googleSubject = googleSubject;
        this.rol = rol;
        this.activo = activo;
        this.mfaSecret = mfaSecret;
        this.mfaHabilitado = mfaHabilitado;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.facebookAccessToken = facebookAccessToken;
        this.facebookAccessTokenExpiresAt = facebookAccessTokenExpiresAt;
        this.facebookScopes = facebookScopes;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public String getMfaSecret() {
        return mfaSecret;
    }

    public boolean isMfaHabilitado() {
        return mfaHabilitado;
    }

    public Usuario conMfa(String secret, boolean habilitado) {
        return new Usuario(
                id, username, email, nombres, apellidos, passwordHash, googleSubject, rol, activo,
                secret, habilitado, provider, providerUserId, facebookAccessToken,
                facebookAccessTokenExpiresAt, facebookScopes);
    }

    public Usuario sinMfa() {
        return conMfa(null, false);
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getFacebookAccessToken() {
        return facebookAccessToken;
    }

    public long getFacebookAccessTokenExpiresAt() {
        return facebookAccessTokenExpiresAt;
    }

    public String getFacebookScopes() {
        return facebookScopes;
    }

    public Usuario conEmail(String email) {
        return new Usuario(
                id, username, email, nombres, apellidos, passwordHash, googleSubject, rol, activo,
                mfaSecret, mfaHabilitado, provider, providerUserId, facebookAccessToken,
                facebookAccessTokenExpiresAt, facebookScopes);
    }

    public Usuario conNombre(String nombres, String apellidos) {
        return new Usuario(
                id, username, email, nombres, apellidos, passwordHash, googleSubject, rol, activo,
                mfaSecret, mfaHabilitado, provider, providerUserId, facebookAccessToken,
                facebookAccessTokenExpiresAt, facebookScopes);
    }

    public Usuario conGoogleSubject(String googleSubject) {
        return new Usuario(
                id, username, email, nombres, apellidos, passwordHash, googleSubject, rol, activo,
                mfaSecret, mfaHabilitado, provider, providerUserId, facebookAccessToken,
                facebookAccessTokenExpiresAt, facebookScopes);
    }

    public Usuario conAutorizacionFacebook(
            String encryptedAccessToken, long expiresAt, String scopes) {
        return new Usuario(
                id, username, email, nombres, apellidos, passwordHash, googleSubject, rol, activo,
                mfaSecret, mfaHabilitado, provider, providerUserId, encryptedAccessToken, expiresAt,
                scopes);
    }

    public Usuario sinAutorizacionFacebook() {
        return new Usuario(
                id, username, email, nombres, apellidos, passwordHash, googleSubject, rol, activo,
                mfaSecret, mfaHabilitado, null, null, null, 0, null);
    }
}
