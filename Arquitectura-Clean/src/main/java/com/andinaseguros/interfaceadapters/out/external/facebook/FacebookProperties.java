package com.andinaseguros.interfaceadapters.out.external.facebook;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.facebook")
public record FacebookProperties(
        String appId,
        String appSecret,
        String redirectUri,
        String authorizationUri,
        String tokenUri,
        String graphBaseUrl,
        String scopes,
        Integer oauthStateTtlSeconds,
        String frontendCallbackUrl,
        Integer loginTicketTtlSeconds,
        String tokenEncryptionKey) {}