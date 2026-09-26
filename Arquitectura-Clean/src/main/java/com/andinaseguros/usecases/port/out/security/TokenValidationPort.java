package com.andinaseguros.usecases.port.out.security;

public interface TokenValidationPort {
    TokenClaims validar(String token);
}
