package com.backendseguros.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Propaga la identidad validada por Spring Security a los servicios (X-User-Id, X-User-Rol) y
 * borra esos encabezados si los mandó el cliente: nadie puede hacerse pasar por otro usuario
 * escribiéndolos a mano. Los servicios igual validan el token por su cuenta.
 */
@Component
public class IdentidadHeadersGlobalFilter implements GlobalFilter, Ordered {
    static final String USER_ID = "X-User-Id";
    static final String USER_ROL = "X-User-Rol";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(JwtAuthenticationToken.class::cast)
                .map(auth -> conIdentidad(exchange, auth.getName(), auth.getToken().getClaimAsString("rol")))
                .switchIfEmpty(Mono.fromSupplier(() -> conIdentidad(exchange, null, null)))
                .flatMap(chain::filter);
    }

    /** Los encabezados de la petición son de solo lectura: se trabaja sobre una copia. */
    private static ServerWebExchange conIdentidad(ServerWebExchange exchange, String userId, String rol) {
        HttpHeaders copia = new HttpHeaders();
        copia.putAll(exchange.getRequest().getHeaders());
        copia.remove(USER_ID);
        copia.remove(USER_ROL);
        if (userId != null) {
            copia.set(USER_ID, userId);
            copia.set(USER_ROL, rol == null ? "" : rol);
        }
        ServerHttpRequest request =
                new ServerHttpRequestDecorator(exchange.getRequest()) {
                    @Override
                    public HttpHeaders getHeaders() {
                        return copia;
                    }
                };
        return exchange.mutate().request(request).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
