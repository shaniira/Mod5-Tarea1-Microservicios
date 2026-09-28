package com.backendseguros.identity.frameworksdrivers.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/**
 * identity-service también es resource server de sus propios tokens (para /api/auth/me y
 * /api/mfa/**). No configura CORS: el navegador solo habla con el gateway, que lo resuelve.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        .requestMatchers(
                                                "/api/auth/**",
                                                "/.well-known/jwks.json",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/actuator/health",
                                                "/actuator/health/**",
                                                "/actuator/prometheus")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        oauth ->
                                oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(SecurityConfig::convertir))
                                        .authenticationEntryPoint(tokenInvalido()))
                .exceptionHandling(e -> e.authenticationEntryPoint(tokenInvalido()))
                .build();
    }

    /** El nombre del usuario es el sub; el rol sale del claim "rol" (ROLE_ADMIN, ...). */
    static JwtAuthenticationToken convertir(Jwt jwt) {
        String rol = jwt.getClaimAsString("rol");
        var authorities =
                rol == null ? List.<SimpleGrantedAuthority>of() : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    /** Mismo cuerpo de error que devolvía el monolito ante un token inválido o vencido. */
    private static AuthenticationEntryPoint tokenInvalido() {
        return (request, response, exception) -> {
            response.setStatus(401);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter()
                    .write(
                            "{\"codigo\":\"TOKEN_INVALIDO\",\"message\":\"La sesión expiró o el"
                                    + " token no es válido. Inicie sesión nuevamente.\"}");
        };
    }
}
