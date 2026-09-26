package com.andinaseguros.usecases.port.out.security;

public record MfaChallenge(String token, long expiraEnSegundos) {}
