package com.backendseguros.identity.usecases.port.out.security;

/**
 * Identidad que viaja en el JWT. customerId solo existe para usuarios CLIENTE que corresponden a
 * un cliente registrado: permite a los demás servicios filtrar por propietario sin consultar a
 * identity (sección 7.1 de la propuesta).
 */
public record AuthenticatedUser(String username, String rol, String customerId) {
    public AuthenticatedUser(String username, String rol) {
        this(username, rol, null);
    }
}
