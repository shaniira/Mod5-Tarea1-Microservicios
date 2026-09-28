package com.backendseguros.identity.interfaceadapters.out.security.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Estado efímero compartido entre réplicas (paso 2.5): guarda un valor con TTL bajo una clave
 * aleatoria y lo consume con GETDEL, que es atómico. Si dos réplicas reciben el mismo ticket a la
 * vez, solo una lo obtiene (un solo uso), igual que hacía el ConcurrentHashMap en memoria.
 */
class RedisEstadoEfimero {
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final String prefijo;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    RedisEstadoEfimero(StringRedisTemplate redis, ObjectMapper json, String prefijo, Duration ttl) {
        this.redis = redis;
        this.json = json;
        this.prefijo = prefijo;
        this.ttl = ttl;
    }

    String guardar(Object valor) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String clave = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(prefijo + clave, escribir(valor), ttl);
        return clave;
    }

    <T> Optional<T> consumir(String clave, Class<T> tipo) {
        if (clave == null || clave.isBlank()) {
            return Optional.empty();
        }
        String valor = redis.opsForValue().getAndDelete(prefijo + clave);
        return Optional.ofNullable(valor).map(v -> leer(v, tipo));
    }

    long ttlSegundos() {
        return ttl.toSeconds();
    }

    private String escribir(Object valor) {
        try {
            return json.writeValueAsString(valor);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el estado efimero", e);
        }
    }

    private <T> T leer(String valor, Class<T> tipo) {
        try {
            return json.readValue(valor, tipo);
        } catch (Exception e) {
            throw new IllegalStateException("Estado efimero corrupto", e);
        }
    }
}
