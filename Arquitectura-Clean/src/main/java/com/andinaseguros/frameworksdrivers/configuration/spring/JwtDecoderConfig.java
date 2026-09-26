package com.andinaseguros.frameworksdrivers.configuration.spring;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWTParser;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Validación de tokens del monolito (paso 2.3 de la ruta). Los tokens RS256 que emite
 * identity-service se verifican con su clave pública (JWKS). Durante la ventana de transición
 * también se aceptan los HS256 que emitía el monolito, para no cerrar las sesiones abiertas; la
 * ventana se cierra con app.jwt.legacy-hs256-enabled=false y después se retira este camino.
 */
@Configuration
public class JwtDecoderConfig {
    private static final Logger log = LoggerFactory.getLogger(JwtDecoderConfig.class);

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${app.jwt.jwks-uri}") String jwksUri,
            @Value("${app.jwt.issuer:andina-identity}") String issuer,
            @Value("${app.jwt.legacy-hs256-enabled:false}") boolean legacyHabilitado,
            @Value("${app.jwt.secret:}") String secretoLegado) {
        // El JWKS se descarga en la primera validación y queda en caché: el monolito arranca
        // aunque identity-service todavía no esté listo.
        NimbusJwtDecoder rs256 = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        rs256.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));

        NimbusJwtDecoder hs256 = null;
        if (legacyHabilitado && secretoLegado != null && secretoLegado.length() >= 32) {
            byte[] clave = secretoLegado.getBytes(StandardCharsets.UTF_8);
            // Misma regla que usaba jjwt (Keys.hmacShaKeyFor) para elegir el algoritmo.
            MacAlgorithm algoritmo =
                    clave.length >= 64
                            ? MacAlgorithm.HS512
                            : clave.length >= 48 ? MacAlgorithm.HS384 : MacAlgorithm.HS256;
            hs256 =
                    NimbusJwtDecoder.withSecretKey(
                                    new SecretKeySpec(clave, "HmacSHA" + algoritmo.getName().substring(2)))
                            .macAlgorithm(algoritmo)
                            .build();
            log.warn("Ventana de transicion abierta: se aceptan tokens HS256/HS384 antiguos del monolito");
        }
        NimbusJwtDecoder legado = hs256;
        return token -> {
            if (esHmac(token)) {
                if (legado == null) {
                    throw new BadJwtException("Los tokens HS256 del monolito ya no se aceptan");
                }
                return legado.decode(token);
            }
            return rs256.decode(token);
        };
    }

    private static boolean esHmac(String token) {
        try {
            return JWSAlgorithm.Family.HMAC_SHA.contains(
                    JWTParser.parse(token).getHeader().getAlgorithm());
        } catch (Exception e) {
            throw new BadJwtException("Token mal formado");
        }
    }
}
