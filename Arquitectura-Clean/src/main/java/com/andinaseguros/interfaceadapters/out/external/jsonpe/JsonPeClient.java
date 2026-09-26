package com.andinaseguros.interfaceadapters.out.external.jsonpe;

import com.andinaseguros.usecases.exception.*;
import java.net.http.HttpTimeoutException;
import org.springframework.http.*;
import org.springframework.web.client.*;

public class JsonPeClient {
    private final RestClient client;
    private final JsonPeProperties properties;

    public JsonPeClient(RestClient client, JsonPeProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    JsonPePlateResponse consultar(String placa) {
        try {
            return client.post()
                    .uri("/api/placa")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new JsonPePlateRequest(placa))
                    .retrieve()
                    .body(JsonPePlateResponse.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new VehicleProviderAuthenticationException("JSONPE_UNAUTHORIZED", e);
        } catch (HttpClientErrorException.BadRequest e) {
            throw new VehicleProviderInvalidRequestException("JSONPE_BAD_REQUEST", e);
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof HttpTimeoutException) {
                throw new VehicleProviderTimeoutException("JSONPE_TIMEOUT", e);
            }
            throw new VehicleProviderUnavailableException("JSONPE_UNAVAILABLE", e);
        } catch (RestClientException e) {
            throw new VehicleProviderUnavailableException("JSONPE_UNAVAILABLE", e);
        }
    }
}
