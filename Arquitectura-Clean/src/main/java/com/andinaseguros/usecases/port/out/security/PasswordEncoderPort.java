package com.andinaseguros.usecases.port.out.security;

public interface PasswordEncoderPort {
    String codificar(String textoPlano);

    boolean coincide(String textoPlano, String hash);
}
