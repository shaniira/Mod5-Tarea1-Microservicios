package com.andinaseguros.identity.usecases.port.out.security;

public interface TokenGeneratorPort {
    String generar(AuthenticatedUser usuario);

    long expirationSeconds();
}
