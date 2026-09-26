package com.andinaseguros.notification.notification;

import com.andinaseguros.notification.config.WhatsAppProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class WhatsAppClient {
    private final RestClient client;
    private final WhatsAppProperties properties;

    public WhatsAppClient(RestClient client, WhatsAppProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public WhatsAppTextResponse enviarTexto(WhatsAppTextRequest request) {
        return client.post()
                .uri("/send/text")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(WhatsAppTextResponse.class);
    }
}
