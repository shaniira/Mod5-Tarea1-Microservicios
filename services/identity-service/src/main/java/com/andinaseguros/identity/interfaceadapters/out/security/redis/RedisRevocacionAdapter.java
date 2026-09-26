package com.andinaseguros.identity.interfaceadapters.out.security.redis;

import com.andinaseguros.identity.usecases.port.out.security.RevocacionPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Claves en Redis (base de identity):
 *
 * <ul>
 *   <li>{@code identity:revocado:usuario:<username>}: dura lo que un token, así cubre todos los
 *       tokens emitidos antes de desactivarlo.
 *   <li>{@code identity:revocado:token:<jti>}: dura hasta que ese token vence.
 * </ul>
 *
 * El gateway lee estas mismas claves (ver RevocacionesRedis en el gateway).
 */
public class RedisRevocacionAdapter implements RevocacionPort {
    public static final String USUARIO = "identity:revocado:usuario:";
    public static final String TOKEN = "identity:revocado:token:";

    private final StringRedisTemplate redis;
    private final Duration vidaToken;
    private final Clock clock;

    public RedisRevocacionAdapter(StringRedisTemplate redis, Duration vidaToken, Clock clock) {
        this.redis = redis;
        this.vidaToken = vidaToken;
        this.clock = clock;
    }

    @Override
    public void revocarUsuario(String username) {
        redis.opsForValue().set(USUARIO + username, clock.instant().toString(), vidaToken);
    }

    @Override
    public void restaurarUsuario(String username) {
        redis.delete(USUARIO + username);
    }

    @Override
    public void revocarToken(String jti, Instant expiraEn) {
        Duration restante = Duration.between(clock.instant(), expiraEn);
        if (!restante.isNegative() && !restante.isZero()) {
            redis.opsForValue().set(TOKEN + jti, "logout", restante);
        }
    }

    @Override
    public boolean estaRevocado(String username, String jti) {
        return Boolean.TRUE.equals(redis.hasKey(USUARIO + username))
                || (jti != null && Boolean.TRUE.equals(redis.hasKey(TOKEN + jti)));
    }
}
