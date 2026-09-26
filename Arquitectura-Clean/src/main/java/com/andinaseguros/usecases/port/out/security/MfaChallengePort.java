package com.andinaseguros.usecases.port.out.security;

public interface MfaChallengePort {
    MfaChallenge crear(AuthenticatedUser usuario);

    AuthenticatedUser consumir(String token);
}
