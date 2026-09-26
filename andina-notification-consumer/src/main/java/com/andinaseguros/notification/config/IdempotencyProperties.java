package com.andinaseguros.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.idempotency")
public record IdempotencyProperties(String consumerName) {}
