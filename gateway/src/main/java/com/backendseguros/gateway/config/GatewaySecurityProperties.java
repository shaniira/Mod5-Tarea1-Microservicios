package com.backendseguros.gateway.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config de seguridad del Gateway, cargada 100% desde variables de entorno / ConfigMap
 * (ver application.yml). Ninguna URL ni secreto queda escrito en el codigo Java.
 *
 * @param jwksUri claves publicas de identity-service (fase 2)
 * @param issuer emisor esperado en los tokens
 */
@ConfigurationProperties(prefix = "app.security")
public record GatewaySecurityProperties(
        String jwksUri,
        String issuer,
        List<String> publicPaths,
        String correlationIdHeader) {
    public GatewaySecurityProperties {
        if (publicPaths == null) {
            publicPaths = List.of();
        }
        if (correlationIdHeader == null || correlationIdHeader.isBlank()) {
            correlationIdHeader = "X-Correlation-Id";
        }
        if (jwksUri == null || jwksUri.isBlank()) {
            throw new IllegalStateException(
                    "app.security.jwks-uri (JWT_JWKS_URI) es obligatorio: el Gateway valida los"
                            + " tokens con las claves publicas de identity-service");
        }
    }
}
