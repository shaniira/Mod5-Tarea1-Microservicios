package com.andinaseguros.interfaceadapters.out.external.facebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.andinaseguros.entities.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class FacebookOAuthAdapterTest {
    @Test
    void construyeUrlDeAutorizacionConValoresDeConfiguracion() {
        var properties = properties("app-id", "app-secret");
        var adapter = new FacebookOAuthAdapter(mock(RestClient.class), properties);

        var url = adapter.authorizationUrl("state value");

        assertThat(url)
                .isEqualTo(
                        "https://facebook.example/oauth?client_id=app-id"
                                + "&redirect_uri=http%3A%2F%2Flocalhost%3A8083%2Fapi%2Fauth%2Ffacebook%2Fcallback"
                                + "&response_type=code&state=state+value&scope=public_profile%2Cemail");
    }

    @Test
    void rechazaConfiguracionSinAppSecretAntesDeLlamarAFacebook() {
        var adapter = new FacebookOAuthAdapter(mock(RestClient.class), properties("app-id", ""));

        assertThatThrownBy(() -> adapter.authorizationUrl("state"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Facebook no está configurado");
    }

    private FacebookProperties properties(String appId, String appSecret) {
        return new FacebookProperties(
                appId,
                appSecret,
                "http://localhost:8083/api/auth/facebook/callback",
                "https://facebook.example/oauth",
                "https://facebook.example/token",
                "https://facebook.example/graph",
                "public_profile,email",
                300,
                "http://localhost:5173/login",
                60,
                "unused-by-these-tests");
    }
}