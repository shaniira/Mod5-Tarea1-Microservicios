package com.backendseguros.gateway.config;

import java.time.Duration;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

@Configuration
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewayConfig {

    /** Un solo PathMatcher compartido por los filtros que necesitan reconocer rutas publicas. */
    @Bean
    PathMatcher pathMatcher() {
        return new AntPathMatcher();
    }

    /**
     * El DNS de Docker responde con un TTL largo y Reactor Netty lo respeta: al recrear un
     * contenedor (nueva IP) el gateway seguia llamando a la IP vieja y respondia 503 hasta
     * reiniciarse. Con un cache corto encuentra la IP nueva en segundos, y al escalar un servicio
     * (varias replicas) empieza a repartir entre todas.
     */
    @Bean
    HttpClientCustomizer resolucionDnsCorta() {
        return client ->
                client.resolver(
                        spec ->
                                spec.cacheMaxTimeToLive(Duration.ofSeconds(10))
                                        .cacheNegativeTimeToLive(Duration.ofSeconds(1)));
    }

    /** Redis del rate limiter: falla rápido si Redis cae (ver {@link RedisSinBloqueo}). */
    @Bean
    LettuceClientConfigurationBuilderCustomizer redisSinBloqueo() {
        return builder -> builder.clientOptions(RedisSinBloqueo.opciones()).commandTimeout(RedisSinBloqueo.TIMEOUT);
    }
}
