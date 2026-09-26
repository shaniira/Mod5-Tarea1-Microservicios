package com.andinaseguros.notification.frameworksdrivers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.whatsapp")
public record WhatsAppProperties(String baseUrl, String token, Integer timeoutSeconds) {}
