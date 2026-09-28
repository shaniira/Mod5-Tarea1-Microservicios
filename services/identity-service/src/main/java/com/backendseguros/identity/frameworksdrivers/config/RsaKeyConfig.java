package com.backendseguros.identity.frameworksdrivers.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.backendseguros.identity.usecases.port.out.security.RevocacionPort;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Par de claves RS256 (paso 2.2). La privada se lee de un archivo PEM (PKCS#8) que en Docker
 * genera el contenedor identity-keygen en un volumen montado solo aquí; nunca está en el
 * repositorio ni en variables de entorno de otros servicios. El kid es la huella (RFC 7638) de la
 * clave pública, así un cambio de clave cambia el kid y los validadores lo detectan.
 */
@Configuration
public class RsaKeyConfig {
    private static final Logger log = LoggerFactory.getLogger(RsaKeyConfig.class);

    @Bean
    RSAKey identityRsaKey(
            @Value("${app.jwt.private-key-file:}") String privateKeyFile,
            @Value("${app.jwt.generate-if-missing:false}") boolean generarSiFalta)
            throws Exception {
        if (privateKeyFile != null && !privateKeyFile.isBlank() && Files.exists(Path.of(privateKeyFile))) {
            return construir(leerClavePrivada(Path.of(privateKeyFile)));
        }
        if (!generarSiFalta) {
            throw new IllegalStateException(
                    "No existe la clave privada JWT (" + privateKeyFile + "). identity-service no"
                            + " arranca sin ella; en Docker la genera el servicio identity-keygen.");
        }
        log.warn(
                "Clave JWT generada en memoria (app.jwt.generate-if-missing=true). Solo para pruebas:"
                        + " cada arranque invalida los tokens anteriores y no sirve con varias replicas.");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return construir((RSAPrivateCrtKey) generator.generateKeyPair().getPrivate());
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey identityRsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(identityRsaKey)));
    }

    /** Valida los tokens propios (para /api/auth/me, /api/mfa/**) con la clave pública local. */
    @Bean
    JwtDecoder jwtDecoder(
            RSAKey identityRsaKey, @Value("${app.jwt.issuer}") String issuer, RevocacionPort revocaciones)
            throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(identityRsaKey.toRSAPublicKey()).build();
        // Además de firma, emisor y vigencia: el token no puede estar revocado (logout o usuario
        // desactivado).
        OAuth2TokenValidator<Jwt> noRevocado =
                jwt ->
                        revocaciones.estaRevocado(jwt.getSubject(), jwt.getId())
                                ? OAuth2TokenValidatorResult.failure(
                                        new OAuth2Error("invalid_token", "Token revocado", null))
                                : OAuth2TokenValidatorResult.success();
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer), noRevocado));
        return decoder;
    }

    private RSAKey construir(RSAPrivateCrtKey privateKey) throws Exception {
        RSAPublicKey publicKey =
                (RSAPublicKey)
                        KeyFactory.getInstance("RSA")
                                .generatePublic(
                                        new RSAPublicKeySpec(
                                                privateKey.getModulus(), privateKey.getPublicExponent()));
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyIDFromThumbprint()
                .build();
    }

    private RSAPrivateCrtKey leerClavePrivada(Path archivo) throws Exception {
        String pem = Files.readString(archivo, StandardCharsets.US_ASCII);
        if (!pem.contains("BEGIN PRIVATE KEY")) {
            throw new IllegalStateException(
                    "La clave JWT debe estar en formato PKCS#8 (BEGIN PRIVATE KEY)");
        }
        String base64 =
                pem.replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");
        return (RSAPrivateCrtKey)
                KeyFactory.getInstance("RSA")
                        .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }
}
