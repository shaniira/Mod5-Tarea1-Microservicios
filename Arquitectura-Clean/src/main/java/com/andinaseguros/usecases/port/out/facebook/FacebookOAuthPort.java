package com.andinaseguros.usecases.port.out.facebook;

import java.time.Instant;
import java.util.Set;

public interface FacebookOAuthPort {
    String authorizationUrl(String state);

    FacebookIdentity exchangeCode(String code);

    record FacebookIdentity(
            String id,
            String email,
            String name,
            String firstName,
            String lastName,
            String accessToken,
            Instant expiresAt,
            Set<String> scopes) {}
}