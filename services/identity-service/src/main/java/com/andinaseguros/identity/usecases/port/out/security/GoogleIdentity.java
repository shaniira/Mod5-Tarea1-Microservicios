package com.andinaseguros.identity.usecases.port.out.security;

public record GoogleIdentity(
        String subject,
        String email,
        boolean emailVerified,
        String name,
        String givenName,
        String familyName,
        String picture) {}
