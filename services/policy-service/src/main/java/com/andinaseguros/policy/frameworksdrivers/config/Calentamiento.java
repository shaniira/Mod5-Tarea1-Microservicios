package com.andinaseguros.policy.frameworksdrivers.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Calentamiento al arrancar (fase 7, prueba de carga y de caos): la primera petición después de un
 * reinicio tardaba más de 5 s (el límite del gateway) y respondía 503, aunque el servicio ya
 * figuraba "sano". Casi todo ese tiempo era inicialización diferida: la primera conexión a MongoDB
 * y la descarga de las claves públicas de identity (JWKS) al validar el primer token.
 *
 * <p>Corre antes de que el servicio se declare listo (readiness), así que el healthcheck y
 * Kubernetes no le mandan tráfico hasta que termina. Si algo falla solo se registra: el servicio
 * arranca igual (por ejemplo, con identity caído valida los tokens cuando identity vuelva).
 */
@Component
class Calentamiento implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(Calentamiento.class);
    /** JWT sin firma válida, con un kid que no existe: obliga a descargar el JWKS y se rechaza. */
    private static final String TOKEN_DE_PRUEBA =
            base64("{\"alg\":\"RS256\",\"typ\":\"JWT\",\"kid\":\"calentamiento\"}") + "."
                    + base64("{\"sub\":\"calentamiento\"}") + ".AA";

    private final MongoTemplate mongo;
    private final JwtDecoder jwtDecoder;

    Calentamiento(MongoTemplate mongo, JwtDecoder jwtDecoder) {
        this.mongo = mongo;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        long inicio = System.currentTimeMillis();
        try {
            mongo.executeCommand(new Document("ping", 1));
        } catch (RuntimeException e) {
            log.warn("Calentamiento: MongoDB no respondió ({})", e.getMessage());
        }
        try {
            jwtDecoder.decode(TOKEN_DE_PRUEBA);
        } catch (JwtException esperado) {
            // Esperado: el token de prueba no es válido. Lo que importa es que se descargó el JWKS.
        } catch (RuntimeException e) {
            log.warn("Calentamiento: no se pudo descargar el JWKS de identity ({})", e.getMessage());
        }
        log.info("Calentamiento listo en {} ms", System.currentTimeMillis() - inicio);
    }

    private static String base64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
