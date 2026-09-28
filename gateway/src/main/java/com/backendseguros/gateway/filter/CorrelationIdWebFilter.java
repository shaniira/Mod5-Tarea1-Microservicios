package com.backendseguros.gateway.filter;

import com.backendseguros.gateway.config.GatewaySecurityProperties;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Asigna el X-Correlation-Id de cada petición (lo genera si no viene) y lo devuelve en la respuesta.
 * Es un WebFilter con la máxima prioridad para correr antes que Spring Security: así también un 401
 * o 403 del gateway lleva el mismo identificador.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    public static final String CORRELATION_ID_ATTRIBUTE = "correlationId";
    private static final Pattern VALIDO = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    private final String headerName;

    public CorrelationIdWebFilter(GatewaySecurityProperties properties) {
        this.headerName = properties.correlationIdHeader();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(headerName);
        // Solo se acepta un identificador con formato razonable: evita inyectar texto arbitrario
        // en los logs de todos los servicios.
        String correlationId =
                incoming != null && VALIDO.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();

        ServerHttpRequest mutatedRequest =
                exchange.getRequest().mutate().header(headerName, correlationId).build();

        exchange.getAttributes().put(CORRELATION_ID_ATTRIBUTE, correlationId);
        exchange.getResponse().getHeaders().set(headerName, correlationId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
}
