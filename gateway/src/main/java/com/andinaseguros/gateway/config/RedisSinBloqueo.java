package com.andinaseguros.gateway.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import java.time.Duration;

/**
 * Fase 7 (prueba de caos con Redis caído): el gateway dejaba de responder a todas las rutas más de
 * 70 s. Lettuce, con la conexión caída, guarda los comandos para enviarlos al reconectar, y los
 * comandos reactivos (rate limiter y lista de revocación) no tenían timeout: la petición esperaba
 * indefinidamente. Con estas opciones un comando falla enseguida si no hay conexión, o a los 2 s
 * si Redis no contesta, y cada uso decide qué hacer: el rate limiter y la revocación dejan pasar la
 * petición (disponibilidad antes que el límite; la firma y el vencimiento del JWT se siguen
 * comprobando).
 */
public final class RedisSinBloqueo {
    /**
     * 2 s: con 500 ms, en la prueba de carga (CPU saturada) Redis tardaba más y el rate limiter
     * dejaba pasar todo justo en el pico (1152 respuestas 200 y solo 5 429 en 10 s). Con Redis
     * caído no se espera: sin conexión el comando se rechaza al instante (REJECT_COMMANDS).
     */
    public static final Duration TIMEOUT = Duration.ofSeconds(2);

    private RedisSinBloqueo() {}

    public static ClientOptions opciones() {
        return ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .timeoutOptions(TimeoutOptions.enabled(TIMEOUT))
                .socketOptions(SocketOptions.builder().connectTimeout(TIMEOUT).build())
                .build();
    }
}
