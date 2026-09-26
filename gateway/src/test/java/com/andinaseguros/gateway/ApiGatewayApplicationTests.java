package com.andinaseguros.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
        properties = {"app.security.jwt-secret=test-secret-key-must-be-at-least-32-bytes-long"})
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
        // Si el contexto de Spring no levanta (beans mal cableados, propiedades invalidas,
        // Redis obligatorio faltante, etc.) esta prueba falla. Requiere Redis embebido/local
        // para el RequestRateLimiter; se documenta en el README de pruebas.
    }
}
