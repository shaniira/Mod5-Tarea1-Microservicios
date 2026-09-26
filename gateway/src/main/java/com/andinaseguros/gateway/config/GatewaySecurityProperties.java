package com.andinaseguros.gateway.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config de seguridad del Gateway, cargada 100% desde variables de entorno / ConfigMap
 * (ver application.yml). Ninguna URL ni secreto queda escrito en el codigo Java.
 *
 * @param jwksUri claves publicas de identity-service (fase 2)
 * @param issuer emisor esperado en los tokens
 * @param legacyHs256Enabled ventana de transicion: acepta tambien los HS256 antiguos
 * @param jwtSecret secreto HS256 antiguo; solo se usa durante la ventana
 */
@ConfigurationProperties(prefix = "app.security")
public record GatewaySecurityProperties(
        String jwksUri,
        String issuer,
        boolean legacyHs256Enabled,
        String jwtSecret,
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
