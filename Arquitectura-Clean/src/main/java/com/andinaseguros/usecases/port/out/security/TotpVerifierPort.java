package com.andinaseguros.usecases.port.out.security;

public interface TotpVerifierPort {
    boolean verificar(String secret, String codigo);
}
