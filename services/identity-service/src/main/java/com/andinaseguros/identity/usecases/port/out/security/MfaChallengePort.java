package com.andinaseguros.identity.usecases.port.out.security;

public interface MfaChallengePort {
    MfaChallenge crear(AuthenticatedUser usuario);

    AuthenticatedUser consumir(String token);
}
