package com.andinaseguros.identity.usecases.port.out.security;

public record MfaChallenge(String token, long expiraEnSegundos) {}
