package com.andinaseguros.gateway.filter;

import com.andinaseguros.gateway.config.GatewaySecurityProperties;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Acepta el {@code X-Correlation-Id} entrante o genera uno nuevo (UUID) y lo propaga a:
 * el request hacia el servicio destino, la respuesta al cliente, y el atributo del exchange
 * que usan {@link AccessLogGlobalFilter} y {@link JwtAuthenticationGlobalFilter} para loguear.
 *
 * <p>Debe ejecutarse antes que cualquier otro filtro (por eso {@link Ordered#HIGHEST_PRECEDENCE}):
 * todo lo demas (logs, JWT, rate limit, circuit breaker) necesita el correlationId ya resuelto.
 */
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_ATTRIBUTE = "correlationId";

    private final String headerName;

    public CorrelationIdGlobalFilter(GatewaySecurityProperties properties) {
        this.headerName = properties.correlationIdHeader();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(headerName);
        String correlationId = (incoming == null || incoming.isBlank()) ? newId() : incoming;

        ServerHttpRequest mutatedRequest =
                exchange.getRequest().mutate().header(headerName, correlationId).build();

        exchange.getAttributes().put(CORRELATION_ID_ATTRIBUTE, correlationId);
        exchange.getResponse().getHeaders().set(headerName, correlationId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
