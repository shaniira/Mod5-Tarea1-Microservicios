package com.andinaseguros.notification.interfaceadapters.out.whatsapp;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Cliente HTTP de la API de WhatsApp de JSON.pe. */
public class WhatsAppClient {
    private final RestClient client;
    private final String token;

    public WhatsAppClient(RestClient client, String token) {
        this.client = client;
        this.token = token;
    }

    public WhatsAppTextResponse enviarTexto(WhatsAppTextRequest request) {
        return client.post()
                .uri("/send/text")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(WhatsAppTextResponse.class);
    }
}
