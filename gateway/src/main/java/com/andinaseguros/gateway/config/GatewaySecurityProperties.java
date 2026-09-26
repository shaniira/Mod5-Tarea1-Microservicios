package com.andinaseguros.gateway.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config de seguridad del Gateway, cargada 100% desde variables de entorno / ConfigMap
 * (ver application.yml). Ninguna URL ni secreto queda escrito en el codigo Java.
 */
@ConfigurationProperties(prefix = "app.security")
public record GatewaySecurityProperties(
        String jwtSecret, List<String> publicPaths, String correlationIdHeader) {

    public GatewaySecurityProperties {
        if (publicPaths == null) {
            publicPaths = List.of();
        }
        if (correlationIdHeader == null || correlationIdHeader.isBlank()) {
            correlationIdHeader = "X-Correlation-Id";
        }
    }
}
