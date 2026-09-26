package com.andinaseguros.identity.interfaceadapters.out.security.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.andinaseguros.identity.entities.exception.ReglaNegocioException;
import com.andinaseguros.identity.usecases.dto.Responses.TokenResponse;
import com.andinaseguros.identity.usecases.port.out.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Simula Redis con un mapa para comprobar el contrato: valor con TTL, un solo uso y el mismo
 * resultado sin importar qué réplica (qué instancia del adaptador) lo consuma.
 */
class RedisEstadoEfimeroTest {
    private final Map<String, String> redis = new HashMap<>();
    private final Map<String, Duration> ttls = new HashMap<>();
    private final StringRedisTemplate template = redisSimulado();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void unDesafioMfaCreadoEnUnaReplicaSeConsumeEnOtraUnaSolaVez() {
        var replicaA = new RedisMfaChallengeAdapter(template, json, 300);
        var replicaB = new RedisMfaChallengeAdapter(template, json, 300);

        var desafio = replicaA.crear(new AuthenticatedUser("admin", "ADMIN", null));

        assertThat(desafio.expiraEnSegundos()).isEqualTo(300);
        assertThat(ttls).containsValue(Duration.ofSeconds(300));
        assertThat(replicaB.consumir(desafio.token())).isEqualTo(new AuthenticatedUser("admin", "ADMIN", null));
        assertThatThrownBy(() -> replicaA.consumir(desafio.token()))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting(e -> ((ReglaNegocioException) e).getCodigo())
                .isEqualTo("MFA_DESAFIO_INVALIDO");
    }

    @Test
    void elTicketDeFacebookConservaElTokenYEsDeUnSoloUso() {
        var adapter = new RedisLoginTicketAdapter(template, json, 60);
        var token = new TokenResponse("jwt", "Bearer", 28800);

        String ticket = adapter.create(token);

        assertThat(adapter.consume(ticket)).contains(token);
        assertThat(adapter.consume(ticket)).isEmpty();
        assertThat(adapter.consume(null)).isEmpty();
    }

    @Test
    void elStateOAuthSoloSirveUnaVez() {
        var adapter = new RedisOAuthStateAdapter(template, json, 300);

        String state = adapter.create();

        assertThat(adapter.consume(state)).isTrue();
        assertThat(adapter.consume(state)).isFalse();
        assertThat(adapter.consume("inventado")).isFalse();
    }

    @SuppressWarnings("unchecked")
    private StringRedisTemplate redisSimulado() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        doAnswer(
                        inv -> {
                            redis.put(inv.getArgument(0), inv.getArgument(1));
                            ttls.put(inv.getArgument(0), inv.getArgument(2));
                            return null;
                        })
                .when(ops)
                .set(anyString(), anyString(), any(Duration.class));
        when(ops.getAndDelete(anyString())).thenAnswer(inv -> redis.remove(inv.<String>getArgument(0)));
        return template;
    }
}
