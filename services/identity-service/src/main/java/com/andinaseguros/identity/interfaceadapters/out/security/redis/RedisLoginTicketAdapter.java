package com.andinaseguros.identity.interfaceadapters.out.security.redis;

import com.andinaseguros.identity.usecases.dto.Responses.TokenResponse;
import com.andinaseguros.identity.usecases.port.out.security.LoginTicketPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Ticket de un solo uso que canjea el frontend tras el login con Facebook (60 s). */
public class RedisLoginTicketAdapter implements LoginTicketPort {
    private final RedisEstadoEfimero estado;

    public RedisLoginTicketAdapter(StringRedisTemplate redis, ObjectMapper json, long ttlSeconds) {
        this.estado =
                new RedisEstadoEfimero(redis, json, "identity:login-ticket:", Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public String create(TokenResponse response) {
        return estado.guardar(response);
    }

    @Override
    public Optional<TokenResponse> consume(String ticket) {
        return estado.consumir(ticket, TokenResponse.class);
    }
}
