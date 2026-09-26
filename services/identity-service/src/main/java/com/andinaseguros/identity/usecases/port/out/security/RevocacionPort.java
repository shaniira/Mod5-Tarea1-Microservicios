package com.andinaseguros.identity.usecases.port.out.security;

import java.time.Instant;

/**
 * Lista de revocación compartida (la consultan identity-service y el gateway). Cada entrada vive lo
 * mismo que un token, así la lista nunca crece sin límite.
 */
public interface RevocacionPort {
    /** Invalida todas las sesiones del usuario (usuario desactivado). */
    void revocarUsuario(String username);

    /** El usuario vuelve a estar activo: sus tokens nuevos valen. */
    void restaurarUsuario(String username);

    /** Invalida un token concreto (logout) hasta que venza. */
    void revocarToken(String jti, Instant expiraEn);

    boolean estaRevocado(String username, String jti);
}
