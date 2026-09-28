package com.backendseguros.identity.interfaceadapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.backendseguros.identity.usecases.port.out.security.AuthenticatedUser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class RsaJwtTokenAdapterTest {
    private final Instant ahora = Instant.now();

    @Test
    void firmaConRs256YSeVerificaSoloConLaClavePublica() throws Exception {
        RSAKey clave = new RSAKeyGenerator(2048).keyIDFromThumbprint(true).algorithm(JWSAlgorithm.RS256).generate();
        var adapter = adapter(clave);

        String token = adapter.generar(new AuthenticatedUser("ana@backendseguros.local", "CLIENTE", "c-123"));

        var jwt = NimbusJwtDecoder.withPublicKey(clave.toRSAPublicKey()).build().decode(token);
        assertThat(jwt.getHeaders()).containsEntry("alg", "RS256").containsEntry("kid", clave.getKeyID());
        assertThat(jwt.getSubject()).isEqualTo("ana@backendseguros.local");
        assertThat(jwt.getClaimAsString("rol")).isEqualTo("CLIENTE");
        assertThat(jwt.getClaimAsStringList("roles")).isEqualTo(List.of("CLIENTE"));
        assertThat(jwt.getClaimAsString("customerId")).isEqualTo("c-123");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("andina-identity");
        assertThat(jwt.getExpiresAt()).isAfter(ahora.plusSeconds(3500));
    }

    @Test
    void elPersonalNoLlevaCustomerId() throws Exception {
        RSAKey clave = new RSAKeyGenerator(2048).keyIDFromThumbprint(true).generate();

        String token = adapter(clave).generar(new AuthenticatedUser("admin", "ADMIN"));

        var jwt = NimbusJwtDecoder.withPublicKey(clave.toRSAPublicKey()).build().decode(token);
        assertThat(jwt.hasClaim("customerId")).isFalse();
    }

    @Test
    void otraClavePublicaNoValidaElToken() throws Exception {
        RSAKey clave = new RSAKeyGenerator(2048).keyIDFromThumbprint(true).generate();
        RSAKey otra = new RSAKeyGenerator(2048).generate();

        String token = adapter(clave).generar(new AuthenticatedUser("admin", "ADMIN"));

        assertThatThrownBy(() -> NimbusJwtDecoder.withPublicKey(otra.toRSAPublicKey()).build().decode(token))
                .isInstanceOf(JwtException.class);
    }

    private RsaJwtTokenAdapter adapter(RSAKey clave) {
        return new RsaJwtTokenAdapter(
                new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(clave))),
                clave.getKeyID(),
                "andina-identity",
                3600,
                () -> ahora);
    }
}
