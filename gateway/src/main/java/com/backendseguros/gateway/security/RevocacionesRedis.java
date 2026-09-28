package com.backendseguros.gateway.security;

import com.backendseguros.gateway.config.RedisSinBloqueo;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import reactor.core.publisher.Mono;

/**
 * Lee la lista de revocación que escribe identity-service (logout y usuarios desactivados). Vive
 * en la base de Redis de identity, distinta de la del rate limiter del gateway, por eso usa una
 * conexión propia de solo lectura.
 *
 * <p>Fase 7: además mantiene una <b>copia local</b> de la lista (una réplica de lectura), refrescada
 * cada pocos segundos. Redis sigue siendo la fuente: si responde, se consulta a él (un logout
 * surte efecto al instante). Si Redis no responde, se responde con la copia en lugar de dejar pasar
 * todo:
 *
 * <ul>
 *   <li>Disponibilidad (AP): el gateway no depende de Redis para atender.
 *   <li>Seguridad: los tokens revocados antes de la caída siguen rechazados. Solo se pierde lo
 *       revocado durante la caída, que identity tampoco puede anotar porque escribe en ese mismo
 *       Redis.
 *   <li>Consistencia eventual acotada: la copia puede estar atrasada como mucho un intervalo de
 *       refresco (métrica {@code gateway_revocaciones_copia_edad_seconds}).
 * </ul>
 */
public class RevocacionesRedis implements DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(RevocacionesRedis.class);
    static final String USUARIO = "identity:revocado:usuario:";
    static final String TOKEN = "identity:revocado:token:";
    private static final String PATRON = "identity:revocado:*";

    private final ReactiveStringRedisTemplate redis;
    private final Runnable alCerrar;
    private final Clock clock;
    private final ScheduledExecutorService refresco;
    private volatile boolean refrescoFallando;
    private final AtomicLong usosDeLaCopia = new AtomicLong();

    private volatile Set<String> copia = Set.of();
    private volatile Instant copiaTomadaEn;
    private volatile boolean usandoCopia;

    public RevocacionesRedis(String host, int port, int database, Duration intervaloRefresco) {
        this(fabrica(host, port, database), intervaloRefresco, Clock.systemUTC());
    }

    private RevocacionesRedis(LettuceConnectionFactory factory, Duration intervaloRefresco, Clock clock) {
        this(new ReactiveStringRedisTemplate(factory), factory::destroy, intervaloRefresco, clock);
    }

    /** Para pruebas: plantilla, acción de cierre, intervalo (null = sin refresco automático) y reloj. */
    RevocacionesRedis(ReactiveStringRedisTemplate redis, Runnable alCerrar, Duration intervaloRefresco, Clock clock) {
        this.redis = redis;
        this.alCerrar = alCerrar;
        this.clock = clock;
        // Un hilo propio (no un Flux.interval: en la prueba en vivo el flujo se detenía tras el primer
        // refresco sin dejar rastro). Cualquier error se registra y la copia anterior se conserva.
        if (intervaloRefresco == null) {
            this.refresco = null;
        } else {
            this.refresco =
                    Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread hilo = new Thread(r, "revocaciones-copia");
                        hilo.setDaemon(true);
                        return hilo;
                    });
            long ms = intervaloRefresco.toMillis();
            this.refresco.scheduleWithFixedDelay(this::refrescarSinFallar, 0, ms, TimeUnit.MILLISECONDS);
        }
    }

    public Mono<Boolean> estaRevocado(String username, String jti) {
        Mono<Boolean> porUsuario = redis.hasKey(USUARIO + username);
        Mono<Boolean> porToken = jti == null ? Mono.just(false) : redis.hasKey(TOKEN + jti);
        return Mono.zip(porUsuario, porToken, (u, t) -> u || t)
                .timeout(RedisSinBloqueo.TIMEOUT)
                .doOnNext(r -> alVolverRedis())
                .onErrorResume(error -> Mono.fromSupplier(() -> segunLaCopia(username, jti, error)));
    }

    /** Relee todas las claves de revocación; si falla, la copia anterior se conserva. */
    Mono<Set<String>> refrescarCopia() {
        return redis.scan(ScanOptions.scanOptions().match(PATRON).count(1000).build())
                .collect(java.util.stream.Collectors.toUnmodifiableSet())
                .timeout(RedisSinBloqueo.TIMEOUT.multipliedBy(2))
                .doOnNext(claves -> {
                    copia = claves;
                    copiaTomadaEn = clock.instant();
                });
    }

    private void refrescarSinFallar() {
        try {
            refrescarCopia().block(RedisSinBloqueo.TIMEOUT.multipliedBy(3));
            if (refrescoFallando) {
                refrescoFallando = false;
                log.info("Copia local de revocaciones al día otra vez ({} revocados)", copia.size());
            }
        } catch (RuntimeException error) {
            if (!refrescoFallando) {
                refrescoFallando = true;
                log.warn("No se pudo refrescar la copia local de revocaciones ({}); se conserva la anterior", error.getMessage());
            }
        }
    }

    private boolean segunLaCopia(String username, String jti, Throwable error) {
        usosDeLaCopia.incrementAndGet();
        if (!usandoCopia) {
            usandoCopia = true;
            log.warn(
                    "Redis no responde ({}): la revocación se decide con la copia local de hace {} s ({} revocados)",
                    error.getClass().getSimpleName(), edadCopiaSegundos(), copia.size());
        }
        return copia.contains(USUARIO + username) || (jti != null && copia.contains(TOKEN + jti));
    }

    private void alVolverRedis() {
        if (usandoCopia) {
            usandoCopia = false;
            log.info("Redis volvió a responder: la revocación se consulta de nuevo en Redis");
        }
    }

    /** Segundos desde el último refresco correcto de la copia (-1 si nunca se tomó). */
    public long edadCopiaSegundos() {
        Instant tomada = copiaTomadaEn;
        return tomada == null ? -1 : Duration.between(tomada, clock.instant()).toSeconds();
    }

    public int revocadosEnCopia() {
        return copia.size();
    }

    public long usosDeLaCopia() {
        return usosDeLaCopia.get();
    }

    @Override
    public void destroy() {
        if (refresco != null) {
            refresco.shutdownNow();
        }
        alCerrar.run();
    }

    private static LettuceConnectionFactory fabrica(String host, int port, int database) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        config.setDatabase(database);
        LettuceClientConfiguration cliente =
                LettuceClientConfiguration.builder()
                        .clientOptions(RedisSinBloqueo.opciones())
                        .commandTimeout(RedisSinBloqueo.TIMEOUT)
                        .build();
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, cliente);
        factory.afterPropertiesSet();
        factory.start();
        return factory;
    }
}
