package com.andinaseguros.interfaceadapters.out.security.google;

import com.andinaseguros.usecases.port.out.security.GoogleIdentity;
import com.andinaseguros.usecases.port.out.security.GoogleIdentityVerifierPort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

public class GoogleIdentityVerifierAdapter implements GoogleIdentityVerifierPort {
    public static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    private final JwtDecoder jwtDecoder;
    private final String clientId;
    private final String issuer;

    public GoogleIdentityVerifierAdapter(JwtDecoder jwtDecoder, String clientId, String issuer) {
        this.jwtDecoder = jwtDecoder;
        this.clientId = clientId;
        this.issuer = issuer;
    }

    @Override
    public GoogleIdentity verificar(String idToken) {
        Jwt jwt = jwtDecoder.decode(idToken);

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
}
