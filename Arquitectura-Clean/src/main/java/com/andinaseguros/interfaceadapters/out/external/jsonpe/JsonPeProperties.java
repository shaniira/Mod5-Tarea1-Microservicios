package com.andinaseguros.interfaceadapters.out.external.jsonpe;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jsonpe")
public record JsonPeProperties(String baseUrl, String token, Integer timeoutSeconds) {}
