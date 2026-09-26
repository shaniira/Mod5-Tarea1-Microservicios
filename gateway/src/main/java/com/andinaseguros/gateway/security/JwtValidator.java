package com.andinaseguros.gateway.security;

import com.andinaseguros.gateway.config.GatewaySecurityProperties;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Valida los JWT en el borde (fase 2): solo tokens RS256 firmados por identity-service, verificados
 * con su clave pública (descargada de /.well-known/jwks.json y cacheada). El gateway no tiene
 * ningún secreto; los tokens HS256 antiguos del monolito ya no se aceptan.
 *
 * <p>El gateway valida para rechazar temprano peticiones sin sesión, pero cada servicio vuelve a
 * validar el token (defensa en profundidad).
 */
@Component
public class JwtValidator {
    private final ReactiveJwtDecoder decoder;

    public JwtValidator(GatewaySecurityProperties properties) {
        NimbusReactiveJwtDecoder rs256 =
                NimbusReactiveJwtDecoder.withJwkSetUri(properties.jwksUri())
                        .jwsAlgorithm(SignatureAlgorithm.RS256)
                        .build();
        rs256.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        this.decoder = rs256;
    }

    /** Emite las claims del token si la firma, el emisor y la vigencia son válidos. */
    public Mono<ValidatedToken> validate(String token) {
        return decoder.decode(token)
                .map(
                        jwt ->
                                jwt.getSubject() == null || jwt.getSubject().isBlank()
                                        ? ValidatedToken.invalid("El token no trae subject")
                                        : ValidatedToken.valid(jwt.getSubject(), jwt.getClaimAsString("rol")))
                .onErrorResume(ex -> Mono.just(ValidatedToken.invalid(ex.getMessage())));
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
