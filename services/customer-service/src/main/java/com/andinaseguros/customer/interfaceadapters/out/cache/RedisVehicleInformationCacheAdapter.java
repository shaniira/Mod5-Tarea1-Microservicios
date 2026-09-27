package com.andinaseguros.customer.interfaceadapters.out.cache;

import com.andinaseguros.customer.usecases.model.VehicleInformation;
import com.andinaseguros.customer.usecases.port.out.vehicle.VehicleInformationCachePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Caché de placas en Redis (paso 3.2): clave {@code placa:<PLACA>}, valor JSON y TTL de 24 h. Si
 * Redis falla, la caché se comporta como vacía y la consulta sigue hacia JSON.pe: la caché acelera
 * y sostiene el servicio, pero nunca es la causa de un error.
 */
public class RedisVehicleInformationCacheAdapter implements VehicleInformationCachePort {
    private static final Logger log = LoggerFactory.getLogger(RedisVehicleInformationCacheAdapter.class);
    static final String PREFIJO = "placa:";

    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final Duration ttl;

    public RedisVehicleInformationCacheAdapter(
            StringRedisTemplate redis, ObjectMapper json, Duration ttl) {
        this.redis = redis;
        this.json = json;
        this.ttl = ttl;
    }

    @Override
    public Optional<VehicleInformation> buscar(String placa) {
        try {
            String valor = redis.opsForValue().get(PREFIJO + placa);
            return valor == null
                    ? Optional.empty()
                    : Optional.of(json.readValue(valor, VehicleInformation.class));
        } catch (Exception error) {
            log.warn("No se pudo leer la caché de placas ({}); se consulta al proveedor", error.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void guardar(VehicleInformation informacion) {
        try {
            redis.opsForValue()
                    .set(PREFIJO + informacion.placa(), json.writeValueAsString(informacion), ttl);
        } catch (Exception error) {
            log.warn("No se pudo guardar la placa {} en la caché: {}", informacion.placa(), error.getMessage());
        }
    }
}
