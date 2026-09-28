package com.backendseguros.identity.interfaceadapters.out.security.redis;

import com.backendseguros.identity.entities.exception.ReglaNegocioException;
import com.backendseguros.identity.usecases.port.out.security.AuthenticatedUser;
import com.backendseguros.identity.usecases.port.out.security.MfaChallenge;
import com.backendseguros.identity.usecases.port.out.security.MfaChallengePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Reemplaza a InMemoryMfaChallengeAdapter: el desafío sobrevive a un cambio de réplica. */
public class RedisMfaChallengeAdapter implements MfaChallengePort {
    private final RedisEstadoEfimero estado;

    public RedisMfaChallengeAdapter(StringRedisTemplate redis, ObjectMapper json, long ttlSeconds) {
        this.estado =
                new RedisEstadoEfimero(redis, json, "identity:mfa-challenge:", Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public MfaChallenge crear(AuthenticatedUser usuario) {
        return new MfaChallenge(estado.guardar(usuario), estado.ttlSegundos());
    }

    /**
     * Redis borra el desafío al vencer el TTL, así que uno expirado y uno inexistente se ven igual:
     * ambos devuelven MFA_DESAFIO_INVALIDO.
     */
    @Override
    public AuthenticatedUser consumir(String token) {
        return estado.consumir(token, AuthenticatedUser.class)
                .orElseThrow(
                        () ->
                                new ReglaNegocioException(
                                        "MFA_DESAFIO_INVALIDO",
                                        "El desafío MFA no es válido, expiró o ya fue utilizado"));
    }
}
