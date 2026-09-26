package com.andinaseguros.interfaceadapters.out.security;

import com.andinaseguros.usecases.port.out.security.*;
import com.andinaseguros.usecases.port.out.time.ClockPort;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

public class JwtTokenAdapter implements TokenGeneratorPort, TokenValidationPort {
    private final SecretKey key;
    private final long expiration;
    private final ClockPort clock;

    public JwtTokenAdapter(String secret, long expiration, ClockPort clock) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.clock = clock;
    }

    public String generar(AuthenticatedUser usuario) {
        var now = clock.now();
        return Jwts.builder()
                .subject(usuario.username())
                .claim("rol", usuario.rol())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiration)))
                .signWith(key)
                .compact();
    }

    public TokenClaims validar(String token) {
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new TokenClaims(claims.getSubject(), claims.get("rol", String.class));
    }

    public long expirationSeconds() {
        return expiration;
    }
}
