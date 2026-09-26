package com.andinaseguros.frameworksdrivers.configuration.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * El monolito solo verifica tokens (fase 2): los firma identity-service con RS256 y aquí se
 * validan con su clave pública (JWKS). No hay ningún secreto compartido; los tokens HS256 antiguos
 * ya no se aceptan (se cerró la ventana de transición del paso 2.3).
 */
@Configuration
public class JwtDecoderConfig {

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${app.jwt.jwks-uri}") String jwksUri,
            @Value("${app.jwt.issuer:andina-identity}") String issuer) {
        // El JWKS se descarga en la primera validación y queda en caché: el monolito arranca
        // aunque identity-service todavía no esté listo.
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSetUri(jwksUri)
                        .jwsAlgorithm(SignatureAlgorithm.RS256)
                        .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }
}
