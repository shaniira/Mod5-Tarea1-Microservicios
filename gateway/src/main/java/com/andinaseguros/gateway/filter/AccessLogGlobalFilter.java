package com.andinaseguros.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Log de acceso de una linea por peticion, con el correlationId ya resuelto por
 * {@link CorrelationIdWebFilter}.
 *
 * <p>Nota de diseno: en WebFlux el mismo request puede saltar entre varios hilos del event
 * loop, asi que el MDC de Logback (basado en ThreadLocal) no es fiable sin cableado adicional
 * (Reactor Context Propagation + Micrometer). Para no prometer una propagacion de MDC que no
 * esta implementada, este filtro escribe el correlationId explicitamente en cada mensaje de
 * log en vez de depender del MDC. Los microservicios de negocio (procesos Spring MVC,
 * thread-per-request) si pueden usar MDC de forma segura; ver seccion 3 (paso 7) de
 * doc/5. Microservicios/d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md.
 */
@Component
public class AccessLogGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger("gateway.access");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        return chain.filter(exchange)
                .doFinally(
                        signal -> {
                            long durationMs = System.currentTimeMillis() - start;
                            String correlationId =
                                    exchange.getAttribute(
                                            CorrelationIdWebFilter.CORRELATION_ID_ATTRIBUTE);
                            var request = exchange.getRequest();
                            var response = exchange.getResponse();
                            log.info(
                                    "correlationId={} method={} path={} status={} durationMs={}"
                                            + " remoteAddr={}",
                                    correlationId,
                                    request.getMethod(),
                                    request.getPath().value(),
                                    response.getStatusCode(),
                                    durationMs,
                                    request.getRemoteAddress() != null
                                            ? request.getRemoteAddress().getAddress().getHostAddress()
                                            : "unknown");
                        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
