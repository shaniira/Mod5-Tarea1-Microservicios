package com.andinaseguros.gateway.web;

import com.andinaseguros.gateway.filter.CorrelationIdGlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

/**
 * Destino del filtro {@code CircuitBreaker} cuando el circuito hacia el backend esta abierto o
 * la llamada agota el timeout. Responde siempre 503 con {@code Retry-After}, como define la
 * tabla de resiliencia de la propuesta de migracion (seccion 6.2,
 * doc/5. Microservicios/c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md).
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/backend")
    public ResponseEntity<ErrorResponse> backendUnavailable(ServerWebExchange exchange) {
        String correlationId = exchange.getAttribute(CorrelationIdGlobalFilter.CORRELATION_ID_ATTRIBUTE);
        ErrorResponse body =
                ErrorResponse.of(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "SERVICE_UNAVAILABLE",
                        "El backend no esta respondiendo ahora mismo. Intenta de nuevo en unos"
                                + " segundos.",
                        "/fallback/backend",
                        correlationId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "10")
                .body(body);
    }
}
