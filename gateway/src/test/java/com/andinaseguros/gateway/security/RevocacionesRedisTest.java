package com.andinaseguros.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Fase 7: copia local de revocaciones para cuando Redis no responde. */
class RevocacionesRedisTest {
    private final ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
    private final AtomicReference<Instant> ahora = new AtomicReference<>(Instant.parse("2026-09-28T10:00:00Z"));
    private final Clock clock =
            new Clock() {
                @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
                @Override public Clock withZone(java.time.ZoneId zone) { return this; }
                @Override public Instant instant() { return ahora.get(); }
            };
    private final RevocacionesRedis revocaciones = new RevocacionesRedis(redis, () -> {}, null, clock);

    @Test
    void conRedisSanoLaFuenteEsRedis() {
        when(redis.hasKey(RevocacionesRedis.USUARIO + "ana")).thenReturn(Mono.just(false));
        when(redis.hasKey(RevocacionesRedis.TOKEN + "j1")).thenReturn(Mono.just(true));

        assertThat(revocaciones.estaRevocado("ana", "j1").block()).isTrue();
        assertThat(revocaciones.usosDeLaCopia()).isZero();
    }

    @Test
    void conRedisCaidoLosRevocadosAntesDeLaCaidaSiguenRechazados() {
        when(redis.scan(any(ScanOptions.class)))
                .thenReturn(Flux.just(RevocacionesRedis.TOKEN + "j1", RevocacionesRedis.USUARIO + "luis"));
        revocaciones.refrescarCopia().block();
        when(redis.hasKey(anyString())).thenReturn(Mono.error(new RedisConnectionFailureException("caído")));

        assertThat(revocaciones.estaRevocado("ana", "j1").block()).isTrue(); // token con logout
        assertThat(revocaciones.estaRevocado("luis", "j9").block()).isTrue(); // usuario desactivado
        assertThat(revocaciones.estaRevocado("ana", "j2").block()).isFalse(); // token vigente: pasa
        assertThat(revocaciones.usosDeLaCopia()).isEqualTo(3);
    }

    @Test
    void siElRefrescoFallaSeConservaLaCopiaAnteriorYSuEdadCrece() {
        when(redis.scan(any(ScanOptions.class))).thenReturn(Flux.just(RevocacionesRedis.TOKEN + "j1"));
        revocaciones.refrescarCopia().block();
        ahora.set(ahora.get().plus(Duration.ofSeconds(40)));
        when(redis.scan(any(ScanOptions.class))).thenReturn(Flux.error(new RedisConnectionFailureException("caído")));

        revocaciones.refrescarCopia().onErrorResume(e -> Mono.empty()).block();

        assertThat(revocaciones.revocadosEnCopia()).isEqualTo(1);
        assertThat(revocaciones.edadCopiaSegundos()).isEqualTo(40);
    }
}
