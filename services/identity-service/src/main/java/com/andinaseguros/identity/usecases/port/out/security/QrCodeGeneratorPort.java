package com.andinaseguros.identity.usecases.port.out.security;

public interface QrCodeGeneratorPort {
    String generarDataUri(String contenido);
}
