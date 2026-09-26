package com.andinaseguros.gateway.security;

import com.andinaseguros.gateway.config.GatewaySecurityProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWTParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Valida los JWT en el borde (fase 2): los RS256 que firma identity-service se verifican con su
 * clave pública, descargada de /.well-known/jwks.json y cacheada. El gateway ya no necesita ningún
 * secreto para validar.
 *
 * <p>Ventana de transición (paso 2.3): mientras app.security.legacy-hs256-enabled=true también se
 * aceptan los tokens HS256 que emitía el monolito, para no cerrar sesiones abiertas.
 *
 * <p>El gateway valida para rechazar temprano peticiones sin sesión, pero cada servicio vuelve a
 * validar el token (defensa en profundidad).
 */
@Component
public class JwtValidator {
    private final ReactiveJwtDecoder rs256;
    private final SecretKey legado;

    public JwtValidator(GatewaySecurityProperties properties) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(properties.jwksUri()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        this.rs256 = decoder;
        String secret = properties.jwtSecret();
        this.legado =
                properties.legacyHs256Enabled()
                                && secret != null
                                && secret.getBytes(StandardCharsets.UTF_8).length >= 32
                        ? Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))
                        : null;
    }

    /** Emite las claims del token si la firma, el emisor y la vigencia son válidos. */
    public Mono<ValidatedToken> validate(String token) {
        final boolean hmac;
        try {
            hmac = JWSAlgorithm.Family.HMAC_SHA.contains(JWTParser.parse(token).getHeader().getAlgorithm());
        } catch (Exception ex) {
            return Mono.just(ValidatedToken.invalid("Token mal formado"));
        }
        if (hmac) {
            return Mono.fromCallable(() -> validarLegado(token));
        }
        return rs256.decode(token)
                .map(
                        jwt ->
                                jwt.getSubject() == null || jwt.getSubject().isBlank()
                                        ? ValidatedToken.invalid("El token no trae subject")
                                        : ValidatedToken.valid(jwt.getSubject(), jwt.getClaimAsString("rol")))
                .onErrorResume(ex -> Mono.just(ValidatedToken.invalid(ex.getMessage())));
    }

    private ValidatedToken validarLegado(String token) {
        if (legado == null) {
            return ValidatedToken.invalid("Los tokens HS256 del monolito ya no se aceptan");
        }
        try {
            var claims = Jwts.parser().verifyWith(legado).build().parseSignedClaims(token).getPayload();
            if (claims.getSubject() == null || claims.getSubject().isBlank()) {
                return ValidatedToken.invalid("El token no trae subject");
            }
            return ValidatedToken.valid(claims.getSubject(), claims.get("rol", String.class));
        } catch (RuntimeException ex) {
            return ValidatedToken.invalid(ex.getMessage());
        }
    }

    public record ValidatedToken(boolean valid, String subject, String rol, String reason) {
        static ValidatedToken valid(String subject, String rol) {
            return new ValidatedToken(true, subject, rol, null);
        }

        static ValidatedToken invalid(String reason) {
            return new ValidatedToken(false, null, null, reason);
        }
    }
}
