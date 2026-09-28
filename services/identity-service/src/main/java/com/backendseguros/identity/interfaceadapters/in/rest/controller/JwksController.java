package com.backendseguros.identity.interfaceadapters.in.rest.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Claves públicas para verificar los JWT (paso 2.2). toPublicJWK() descarta la parte privada: la
 * clave privada nunca sale de identity-service.
 */
@RestController
public class JwksController {
    private final Map<String, Object> jwks;

    public JwksController(RSAKey identityRsaKey) {
        this.jwks = new JWKSet(identityRsaKey.toPublicJWK()).toJSONObject();
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(jwks);
    }
}
