package com.andinaseguros.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
}
