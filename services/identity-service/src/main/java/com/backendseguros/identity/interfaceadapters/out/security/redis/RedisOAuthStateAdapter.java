package com.backendseguros.identity.interfaceadapters.out.security.redis;

import com.backendseguros.identity.usecases.port.out.facebook.OAuthStatePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * state OAuth de Facebook (protección CSRF, 300 s). El callback puede llegar a una réplica distinta
 * de la que inició el login: por eso vive en Redis y no en memoria.
 */
public class RedisOAuthStateAdapter implements OAuthStatePort {
    private final RedisEstadoEfimero estado;

    public RedisOAuthStateAdapter(StringRedisTemplate redis, ObjectMapper json, long ttlSeconds) {
        this.estado =
                new RedisEstadoEfimero(redis, json, "identity:oauth-state:", Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public String create() {
        return estado.guardar(Boolean.TRUE);
    }

    @Override
    public boolean consume(String state) {
        return estado.consumir(state, Boolean.class).isPresent();
    }
}
