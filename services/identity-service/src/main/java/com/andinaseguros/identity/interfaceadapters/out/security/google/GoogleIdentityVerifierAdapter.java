package com.andinaseguros.identity.interfaceadapters.out.security.google;

import com.andinaseguros.identity.usecases.exception.ProveedorIdentidadNoDisponibleException;
import com.andinaseguros.identity.usecases.port.out.security.GoogleIdentity;
import com.andinaseguros.identity.usecases.port.out.security.GoogleIdentityVerifierPort;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Valida el ID token de Google contra sus claves públicas (JWKS). Las claves se cachean en el
 * JwtDecoder (ver IdentityServiceConfig), así que Google solo se consulta al arrancar o cuando rota
 * sus claves. El circuit breaker "google" cuenta solo las fallas al obtener esas claves, no los
 * tokens inválidos (paso 2.8).
 */
public class GoogleIdentityVerifierAdapter implements GoogleIdentityVerifierPort {
    public static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    private final JwtDecoder jwtDecoder;
    private final String clientId;
    private final String issuer;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;

    public GoogleIdentityVerifierAdapter(JwtDecoder jwtDecoder, String clientId, String issuer) {
        this(jwtDecoder, clientId, issuer, CircuitBreaker.ofDefaults("google"), Bulkhead.ofDefaults("google"));
    }

    public GoogleIdentityVerifierAdapter(
            JwtDecoder jwtDecoder, String clientId, String issuer, CircuitBreaker circuitBreaker, Bulkhead bulkhead) {
        this.jwtDecoder = jwtDecoder;
        this.clientId = clientId;
        this.issuer = issuer;
        this.circuitBreaker = circuitBreaker;
        this.bulkhead = bulkhead;
    }

    @Override
    public GoogleIdentity verificar(String idToken) {
        Jwt jwt = decodificar(idToken);
        if (!issuer.equals(jwt.getClaimAsString("iss"))) {
            throw new JwtException("Issuer de Google inválido");
        }
        if (jwt.getAudience() == null || !jwt.getAudience().contains(clientId)) {
            throw new JwtException("Audience de Google inválida");
        }
        if (jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new JwtException("Token de Google sin subject");
        }
        return new GoogleIdentity(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                jwt.getClaimAsString("picture"));
    }

    private Jwt decodificar(String idToken) {
        try {
            return Bulkhead.decorateSupplier(
                            bulkhead, CircuitBreaker.decorateSupplier(circuitBreaker, () -> decodificarClasificando(idToken)))
                    .get();
        } catch (CallNotPermittedException | BulkheadFullException abierto) {
            throw new ProveedorIdentidadNoDisponibleException("Google", abierto);
        } catch (GoogleSinRespuesta caido) {
            throw new ProveedorIdentidadNoDisponibleException("Google", caido.getCause());
        } catch (TokenInvalido invalido) {
            throw invalido.causa;
        }
    }

    /** Separa "el token es malo" (no cuenta para el circuito) de "Google no responde" (sí cuenta). */
    private Jwt decodificarClasificando(String idToken) {
        try {
            return jwtDecoder.decode(idToken);
        } catch (BadJwtException invalido) {
            throw new TokenInvalido(invalido);
        } catch (JwtException error) {
            // JwtDecoder envuelve en JwtException los errores al descargar el JWKS (timeout, DNS,
            // 5xx). Esos son los que abren el circuito.
            throw new GoogleSinRespuesta(error);
        }
    }

    /** Fallo que cuenta para el circuit breaker (configurado en record-exceptions). */
    public static final class GoogleSinRespuesta extends RuntimeException {
        GoogleSinRespuesta(Throwable cause) {
            super(cause.getMessage(), cause);
        }
    }

    /** Token malo: el circuit breaker lo ignora (ignore-exceptions). */
    public static final class TokenInvalido extends RuntimeException {
        private final JwtException causa;

        TokenInvalido(JwtException causa) {
            super(causa.getMessage(), causa);
            this.causa = causa;
        }
    }
}
