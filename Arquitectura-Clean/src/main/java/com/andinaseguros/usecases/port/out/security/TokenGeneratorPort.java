package com.andinaseguros.usecases.port.out.security;

public interface TokenGeneratorPort {
    String generar(AuthenticatedUser usuario);

    long expirationSeconds();
}
