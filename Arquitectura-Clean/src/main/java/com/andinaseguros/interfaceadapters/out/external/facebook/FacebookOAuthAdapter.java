package com.andinaseguros.interfaceadapters.out.external.facebook;

import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.usecases.port.out.facebook.FacebookOAuthPort;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

public class FacebookOAuthAdapter implements FacebookOAuthPort {
    private static final Logger log = LoggerFactory.getLogger(FacebookOAuthAdapter.class);
    private final RestClient client;
    private final FacebookProperties properties;

    public FacebookOAuthAdapter(RestClient client, FacebookProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public String authorizationUrl(String state) {
        requireConfiguration();
        return properties.authorizationUri() + "?client_id=" + encode(properties.appId())
                + "&redirect_uri=" + encode(properties.redirectUri()) + "&response_type=code&state="
                + encode(state) + "&scope=" + encode(properties.scopes());
    }

    public FacebookIdentity exchangeCode(String code) {
        requireConfiguration();
        try {
            URI tokenUri = UriComponentsBuilder.fromUriString(properties.tokenUri())
                    .queryParam("client_id", properties.appId())
                    .queryParam("client_secret", properties.appSecret())
                    .queryParam("redirect_uri", properties.redirectUri())
                    .queryParam("code", code)
                    .build()
                    .toUri();
            TokenResponse token = client.get().uri(tokenUri).retrieve().body(TokenResponse.class);
            if (token == null || token.access_token() == null) throw new IllegalStateException();
            URI profileUri = UriComponentsBuilder.fromUriString(properties.graphBaseUrl() + "/me")
                    .queryParam("fields", "id,name,first_name,last_name,email")
                    .queryParam("access_token", token.access_token())
                    .build()
                    .toUri();
            ProfileResponse profile = client.get().uri(profileUri).retrieve().body(ProfileResponse.class);
            if (profile == null) throw new IllegalStateException();
                URI permissionsUri = UriComponentsBuilder.fromUriString(properties.graphBaseUrl() + "/me/permissions")
                        .queryParam("access_token", token.access_token())
                        .build()
                        .toUri();
                PermissionsResponse permissions = client.get().uri(permissionsUri)
                    .retrieve().body(PermissionsResponse.class);
                Set<String> scopes = permissions == null || permissions.data() == null ? Set.of()
                    : permissions.data().stream().filter(permission -> "granted".equals(permission.status()))
                        .map(PermissionData::permission).collect(java.util.stream.Collectors.toUnmodifiableSet());
            return new FacebookIdentity(profile.id(), profile.email(), profile.name(), profile.first_name(),
                    profile.last_name(), token.access_token(),
                    token.expires_in() == null ? null : Instant.now().plusSeconds(token.expires_in()), scopes);
        } catch (RestClientResponseException exception) {
            log.warn(
                    "Facebook rechazó la solicitud OAuth ({} {}): {}",
                    exception.getStatusCode().value(),
                    exception.getStatusText(),
                    exception.getResponseBodyAsString());
            throw new ReglaNegocioException("FACEBOOK_NO_DISPONIBLE", "No fue posible validar Facebook");
        } catch (RestClientException | IllegalStateException exception) {
            log.warn("No fue posible completar el intercambio OAuth con Facebook", exception);
            throw new ReglaNegocioException("FACEBOOK_NO_DISPONIBLE", "No fue posible validar Facebook");
        }
    }

    private void requireConfiguration() {
        if (isBlank(properties.appId()) || isBlank(properties.appSecret()) || isBlank(properties.redirectUri())
                || isBlank(properties.authorizationUri()) || isBlank(properties.tokenUri()) || isBlank(properties.graphBaseUrl())) {
            throw new ReglaNegocioException("FACEBOOK_CONFIGURACION_INVALIDA", "Facebook no está configurado");
        }
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private record TokenResponse(String access_token, Long expires_in) {}
    private record ProfileResponse(String id, String name, String first_name, String last_name, String email) {}
    private record PermissionsResponse(List<PermissionData> data) {}
    private record PermissionData(String permission, String status) {}
}