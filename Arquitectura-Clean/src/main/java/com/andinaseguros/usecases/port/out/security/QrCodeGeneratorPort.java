package com.andinaseguros.usecases.port.out.security;

public interface QrCodeGeneratorPort {
    String generarDataUri(String contenido);
}
