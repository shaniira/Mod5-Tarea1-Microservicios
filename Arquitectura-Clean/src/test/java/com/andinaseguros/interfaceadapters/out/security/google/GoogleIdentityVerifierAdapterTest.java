package com.andinaseguros.interfaceadapters.out.security.google;

import static org.assertj.core.api.Assertions.*;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class GoogleIdentityVerifierAdapterTest {
    private static final String CLIENT_ID = "test-client-id.apps.googleusercontent.com";
    private static final String ISSUER = "https://accounts.google.com";

    private KeyPair clavesGoogle;
    private GoogleIdentityVerifierAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        clavesGoogle = generarParDeClaves();
        JwtDecoder jwtDecoder =
                NimbusJwtDecoder.withPublicKey((RSAPublicKey) clavesGoogle.getPublic()).build();
        adapter = new GoogleIdentityVerifierAdapter(jwtDecoder, CLIENT_ID, ISSUER);
    }

    @Test
    void verificaUnTokenValidoYExtraeLaIdentidad() throws Exception {
        String token =
                firmarToken(
                        clavesGoogle,
                        ISSUER,
                        CLIENT_ID,
                        "102938475610293847561",
                        "usuario@gmail.com",
                        true,
                        "Usuario Demo",
                        "https://picture.example/u.png",
                        Instant.now().plusSeconds(3600));

        var identidad = adapter.verificar(token);

        assertThat(identidad.subject()).isEqualTo("102938475610293847561");
        assertThat(identidad.email()).isEqualTo("usuario@gmail.com");
        assertThat(identidad.emailVerified()).isTrue();
        assertThat(identidad.name()).isEqualTo("Usuario Demo");
        assertThat(identidad.picture()).isEqualTo("https://picture.example/u.png");
    }

    @Test
    void rechazaTokenFirmadoConOtraClavePrivada() throws Exception {
        KeyPair clavesAtacante = generarParDeClaves();
        String token =
                firmarToken(
                        clavesAtacante,
                        ISSUER,
                        CLIENT_ID,
                        "sub",
                        "e@x.com",
                        true,
                        "n",
                        "p",
                        Instant.now().plusSeconds(3600));

        assertThatThrownBy(() -> adapter.verificar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaAudienceQueNoCoincideConNuestroClientId() throws Exception {
        String token =
                firmarToken(
                        clavesGoogle,
                        ISSUER,
                        "otra-aplicacion.apps.googleusercontent.com",
                        "sub",
                        "e@x.com",
                        true,
                        "n",
                        "p",
                        Instant.now().plusSeconds(3600));

        assertThatThrownBy(() -> adapter.verificar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaIssuerQueNoEsDeGoogle() throws Exception {
        String token =
                firmarToken(
                        clavesGoogle,
                        "https://issuer-falso.com",
                        CLIENT_ID,
                        "sub",
                        "e@x.com",
                        true,
                        "n",
                        "p",
                        Instant.now().plusSeconds(3600));

        assertThatThrownBy(() -> adapter.verificar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaTokenVencido() throws Exception {
        String token =
                firmarToken(
                        clavesGoogle,
                        ISSUER,
                        CLIENT_ID,
                        "sub",
                        "e@x.com",
                        true,
                        "n",
                        "p",
                        Instant.now().minusSeconds(3600));

        assertThatThrownBy(() -> adapter.verificar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void noRechazaPorSuCuentaUnEmailNoVerificado() throws Exception {
        String token =
                firmarToken(
                        clavesGoogle,
                        ISSUER,
                        CLIENT_ID,
                        "sub",
                        "e@x.com",
                        false,
                        "n",
                        "p",
                        Instant.now().plusSeconds(3600));

        var identidad = adapter.verificar(token);

        assertThat(identidad.emailVerified()).isFalse();
    }

    private KeyPair generarParDeClaves() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String firmarToken(
            KeyPair firmante,
            String iss,
            String aud,
            String sub,
            String email,
            boolean emailVerified,
            String name,
            String picture,
            Instant expiracion)
            throws Exception {
        JWTClaimsSet claims =
                new JWTClaimsSet.Builder()
                        .issuer(iss)
                        .audience(aud)
                        .subject(sub)
                        .claim("email", email)
                        .claim("email_verified", emailVerified)
                        .claim("name", name)
                        .claim("picture", picture)
                        .issueTime(Date.from(Instant.now().minusSeconds(60)))
                        .expirationTime(Date.from(expiracion))
                        .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) firmante.getPrivate()));
        return jwt.serialize();
    }
}
