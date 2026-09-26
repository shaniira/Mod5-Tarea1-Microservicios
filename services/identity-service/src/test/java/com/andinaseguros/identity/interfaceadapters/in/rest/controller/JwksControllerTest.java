package com.andinaseguros.identity.interfaceadapters.in.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwksControllerTest {
    @Test
    void publicaSoloLaParteGeneralDeLaClave() throws Exception {
        RSAKey clave =
                new RSAKeyGenerator(2048)
                        .keyUse(KeyUse.SIGNATURE)
                        .algorithm(JWSAlgorithm.RS256)
                        .keyIDFromThumbprint(true)
                        .generate();

        Map<String, Object> jwks = new JwksController(clave).jwks().getBody();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
        assertThat(keys).hasSize(1);
        assertThat(keys.get(0))
                .containsEntry("kty", "RSA")
                .containsEntry("kid", clave.getKeyID())
                .containsKeys("n", "e")
                .doesNotContainKeys("d", "p", "q", "dp", "dq", "qi");
    }
}
