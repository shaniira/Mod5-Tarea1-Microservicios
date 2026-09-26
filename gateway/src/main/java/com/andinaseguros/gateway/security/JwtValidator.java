package com.andinaseguros.gateway.security;

import com.andinaseguros.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Valida la firma y vigencia del JWT emitido hoy por el backend (HS256, secreto compartido).
 *
 * <p>Decision tecnica: el Gateway NO usa Spring Security OAuth2 Resource Server con JWKS
 * porque todavia no existe identity-service (fase 2 de la migracion) exponiendo claves
 * publicas RS256 en {@code /.well-known/jwks.json}. Mientras tanto, tanto el backend
 * (JwtTokenAdapter, HS256) como este Gateway comparten el mismo secreto por variable de
 * entorno ({@code JWT_SECRET}). Cuando identity-service exista, este validador se reemplaza
 * por un {@code ReactiveJwtDecoder} basado en JWKS y deja de compartirse el secreto. Este
 * cambio de tecnologia esta documentado y justificado en
 * doc/5. Microservicios/e_IMPLEMENTACION-API-GATEWAY-FASE0.md.
 *
 * <p>El Gateway valida el token para poder rechazar temprano peticiones sin sesion (evita
 * saturar al backend), pero el backend sigue validando el mismo token de forma independiente
 * (defensa en profundidad): el Gateway nunca es la unica barrera de seguridad.
 */
@Component
public class JwtValidator {

    private final SecretKey key;

    public JwtValidator(GatewaySecurityProperties properties) {
        String secret = properties.jwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET no configurado o menor a 32 bytes; el Gateway no puede arrancar"
                            + " sin un secreto valido (evita repetir el riesgo S3 del analisis"
                            + " de arquitectura).");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Devuelve las claims del token si la firma y la expiracion son validas. */
    public ValidatedToken validate(String token) {
        try {
            var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String subject = claims.getSubject();
            String rol = claims.get("rol", String.class);
            if (subject == null || subject.isBlank()) {
                return ValidatedToken.invalid("El token no trae subject");
            }
            return ValidatedToken.valid(subject, rol);
        } catch (JwtException | IllegalArgumentException ex) {
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
