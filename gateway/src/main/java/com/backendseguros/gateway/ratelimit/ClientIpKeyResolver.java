package com.backendseguros.gateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Resuelve la clave del rate limiter por IP del cliente, no por usuario autenticado: el login y
 * la verificacion MFA (rutas que este resolver protege) ocurren precisamente ANTES de tener un
 * JWT, asi que no hay principal en el que apoyarse (a diferencia del KeyResolver por defecto de
 * Spring Cloud Gateway).
 *
 * <p>Detras de un Ingress/NGINX, la IP real del cliente llega en {@code X-Forwarded-For}; se usa
 * ese valor cuando esta presente y se cae a la IP remota de la conexion en el resto de los casos
 * (por ejemplo, pruebas locales sin Ingress delante).
 */
@Configuration
public class ClientIpKeyResolver {

    @Bean
    KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(resolveClientIp(exchange));
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("Forwarded");
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor;
        }
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }
}
