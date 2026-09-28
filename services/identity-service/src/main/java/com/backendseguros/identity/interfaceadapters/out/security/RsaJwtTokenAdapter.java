package com.backendseguros.identity.interfaceadapters.out.security;

import com.backendseguros.identity.usecases.port.out.security.AuthenticatedUser;
import com.backendseguros.identity.usecases.port.out.security.TokenGeneratorPort;
import com.backendseguros.identity.usecases.port.out.time.ClockPort;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/**
 * Firma el JWT con RS256 (paso 2.2). La clave privada solo existe en identity-service; los demás
 * servicios verifican con la clave pública publicada en /.well-known/jwks.json.
 *
 * <p>Claims: sub (username), rol (lo lee el frontend), roles, customerId (solo CLIENTE), iss, iat,
 * exp y jti. El header lleva el kid para que los validadores elijan la clave correcta.
 */
public class RsaJwtTokenAdapter implements TokenGeneratorPort {
    private final JwtEncoder encoder;
    private final String keyId;
    private final String issuer;
    private final long expirationSeconds;
    private final ClockPort clock;

    public RsaJwtTokenAdapter(
            JwtEncoder encoder, String keyId, String issuer, long expirationSeconds, ClockPort clock) {
        this.encoder = encoder;
        this.keyId = keyId;
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
        this.clock = clock;
    }

    @Override
    public String generar(AuthenticatedUser usuario) {
        var now = clock.now();
        JwtClaimsSet.Builder claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .subject(usuario.username())
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(expirationSeconds))
                        .id(UUID.randomUUID().toString())
                        .claim("rol", usuario.rol())
                        .claim("roles", List.of(usuario.rol()));
        if (usuario.customerId() != null) {
            claims.claim("customerId", usuario.customerId());
        }
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyId).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    @Override
    public long expirationSeconds() {
        return expirationSeconds;
    }
}
