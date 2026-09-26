package com.andinaseguros.gateway.security;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

/**
 * Lee la lista de revocación que escribe identity-service (logout y usuarios desactivados). Vive
 * en la base de Redis de identity, distinta de la del rate limiter del gateway, por eso usa una
 * conexión propia de solo lectura.
 *
 * <p>Si Redis no responde, el token se considera válido (se prioriza la disponibilidad): la firma y
 * el vencimiento se siguen comprobando.
 */
public class RevocacionesRedis implements DisposableBean {
    static final String USUARIO = "identity:revocado:usuario:";
    static final String TOKEN = "identity:revocado:token:";

    private final LettuceConnectionFactory factory;
    private final ReactiveStringRedisTemplate redis;

    public RevocacionesRedis(String host, int port, int database) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        config.setDatabase(database);
        this.factory = new LettuceConnectionFactory(config);
        this.factory.afterPropertiesSet();
        this.factory.start();
        this.redis = new ReactiveStringRedisTemplate(factory);
    }

    public Mono<Boolean> estaRevocado(String username, String jti) {
        Mono<Boolean> porUsuario = redis.hasKey(USUARIO + username);
        Mono<Boolean> porToken = jti == null ? Mono.just(false) : redis.hasKey(TOKEN + jti);
        return Mono.zip(porUsuario, porToken, (u, t) -> u || t).onErrorReturn(false);
    }

    @Override
    public void destroy() {
        factory.destroy();
    }
}
