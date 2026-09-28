package com.backendseguros.gateway.security;

import com.backendseguros.gateway.config.GatewaySecurityProperties;
import com.backendseguros.gateway.filter.CorrelationIdWebFilter;
import com.backendseguros.gateway.web.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * El gateway es OAuth2 Resource Server (Spring Security WebFlux). Valida los JWT RS256 de
 * identity-service con su JWKS, rechaza los revocados (logout o usuario desactivado) y deja pasar
 * sin token solo las rutas públicas.
 *
 * <p>No reemplaza la validación de cada servicio: ellos vuelven a validar el token y aplican sus
 * reglas por rol y propietario (defensa en profundidad).
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    RevocacionesRedis revocacionesRedis(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${app.security.revocation-redis-database:1}") int database,
            @Value("${app.security.revocation-refresh:5s}") Duration refresco,
            MeterRegistry metricas) {
        RevocacionesRedis revocaciones = new RevocacionesRedis(host, port, database, refresco);
        // Fase 7: estado de la copia local de revocaciones (alerta CopiaRevocacionesAtrasada).
        Gauge.builder("gateway.revocaciones.copia.edad", revocaciones, RevocacionesRedis::edadCopiaSegundos)
                .description("Segundos desde el último refresco correcto de la copia local de revocaciones")
                .baseUnit("seconds")
                .register(metricas);
        Gauge.builder("gateway.revocaciones.copia.revocados", revocaciones, RevocacionesRedis::revocadosEnCopia)
                .description("Tokens y usuarios revocados en la copia local")
                .register(metricas);
        FunctionCounter.builder("gateway.revocaciones.copia.usos", revocaciones, RevocacionesRedis::usosDeLaCopia)
                .description("Veces que se decidió con la copia local porque Redis no respondió")
                .register(metricas);
        return revocaciones;
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(GatewaySecurityProperties properties, RevocacionesRedis revocaciones) {
        NimbusReactiveJwtDecoder rs256 =
                NimbusReactiveJwtDecoder.withJwkSetUri(properties.jwksUri())
                        .jwsAlgorithm(SignatureAlgorithm.RS256)
                        .build();
        rs256.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return token ->
                rs256.decode(token)
                        .flatMap(
                                jwt ->
                                        revocaciones
                                                .estaRevocado(jwt.getSubject(), jwt.getId())
                                                .flatMap(
                                                        revocado ->
                                                                revocado
                                                                        ? Mono.error(new BadJwtException("Token revocado"))
                                                                        : Mono.just(jwt)));
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            GatewaySecurityProperties properties,
            GlobalCorsProperties corsProperties,
            ObjectMapper objectMapper) {
        ServerAuthenticationEntryPoint noAutenticado =
                (exchange, ex) ->
                        escribir(exchange, objectMapper, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", mensaje(exchange));
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                // Mismas reglas CORS que spring.cloud.gateway.globalcors: así también un 401 del
                // gateway llega al navegador con sus cabeceras CORS.
                .cors(cors -> cors.configurationSource(corsSource(corsProperties)))
                .authorizeExchange(
                        auth ->
                                auth.pathMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        .pathMatchers(properties.publicPaths().toArray(String[]::new))
                                        .permitAll()
                                        .anyExchange()
                                        .authenticated())
                .oauth2ResourceServer(
                        oauth ->
                                oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(GatewaySecurityConfig::convertir))
                                        .authenticationEntryPoint(noAutenticado))
                .exceptionHandling(e -> e.authenticationEntryPoint(noAutenticado))
                .build();
    }

    /** El rol viene en el claim "rol" (ROLE_ADMIN, ...), igual que en los servicios. */
    static Mono<JwtAuthenticationToken> convertir(org.springframework.security.oauth2.jwt.Jwt jwt) {
        String rol = jwt.getClaimAsString("rol");
        var authorities = rol == null ? List.<SimpleGrantedAuthority>of() : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
        return Mono.just(new JwtAuthenticationToken(jwt, authorities, jwt.getSubject()));
    }

    private static UrlBasedCorsConfigurationSource corsSource(GlobalCorsProperties corsProperties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        corsProperties.getCorsConfigurations().forEach(source::registerCorsConfiguration);
        return source;
    }

    private static String mensaje(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        return authorization == null || !authorization.startsWith("Bearer ")
                ? "Falta el encabezado Authorization: Bearer <token>"
                : "Token invalido, expirado o revocado";
    }

    private static Mono<Void> escribir(
            ServerWebExchange exchange, ObjectMapper json, HttpStatus status, String error, String mensaje) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String correlationId = exchange.getAttribute(CorrelationIdWebFilter.CORRELATION_ID_ATTRIBUTE);
        ErrorResponse body =
                ErrorResponse.of(status.value(), error, mensaje, exchange.getRequest().getPath().value(), correlationId);
        byte[] bytes;
        try {
            bytes = json.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = ("{\"error\":\"" + error + "\"}").getBytes(StandardCharsets.UTF_8);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
