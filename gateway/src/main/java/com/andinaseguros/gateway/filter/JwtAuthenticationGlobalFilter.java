package com.andinaseguros.gateway.filter;

import com.andinaseguros.gateway.config.GatewaySecurityProperties;
import com.andinaseguros.gateway.security.JwtValidator;
import com.andinaseguros.gateway.web.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Puerta de autenticacion del Gateway: exige un JWT valido para toda ruta que no este en la
 * lista de rutas publicas (mismas rutas publicas que ya declara SecurityConfig del backend:
 * /api/auth/**, swagger, actuator/health).
 *
 * <p>Esto NO reemplaza la validacion del backend. Es una barrera adicional para rechazar
 * temprano trafico sin sesion (menos carga en el backend) y para propagar identidad
 * (X-User-Id, X-User-Rol) a los servicios destino, que igual deben volver a autorizar segun
 * sus propias reglas (ver regla 5 "No poner logica de negocio en el API Gateway" y la nota de
 * seguridad "No confiar exclusivamente en que el Gateway proteja toda la seguridad").
 */
@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGlobalFilter.class);

    private final JwtValidator jwtValidator;
    private final PathMatcher pathMatcher;
    private final GatewaySecurityProperties properties;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationGlobalFilter(
            JwtValidator jwtValidator,
            PathMatcher pathMatcher,
            GatewaySecurityProperties properties,
            ObjectMapper objectMapper) {
        this.jwtValidator = jwtValidator;
        this.pathMatcher = pathMatcher;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (request.getMethod() == HttpMethod.OPTIONS || isPublicPath(request.getPath().value())) {
            return chain.filter(exchange);
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange, "Falta el encabezado Authorization: Bearer <token>");
        }

        String token = authorization.substring("Bearer ".length()).trim();
        JwtValidator.ValidatedToken result = jwtValidator.validate(token);
        if (!result.valid()) {
            log.debug("Token rechazado en el Gateway: {}", result.reason());
            return unauthorized(exchange, "Token invalido o expirado");
        }

        ServerHttpRequest mutatedRequest =
                request.mutate()
                        .header("X-User-Id", result.subject())
                        .header("X-User-Rol", result.rol() != null ? result.rol() : "")
                        .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path) {
        return properties.publicPaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String correlationId = exchange.getAttribute(CorrelationIdGlobalFilter.CORRELATION_ID_ATTRIBUTE);
        ErrorResponse body =
                ErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "UNAUTHORIZED",
                        message,
                        exchange.getRequest().getPath().value(),
                        correlationId);

        byte[] bytes = writeBytes(body);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private byte[] writeBytes(ErrorResponse body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (Exception ex) {
            return ("{\"error\":\"UNAUTHORIZED\"}").getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
