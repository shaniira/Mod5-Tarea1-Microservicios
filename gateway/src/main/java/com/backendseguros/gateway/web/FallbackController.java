package com.backendseguros.gateway.web;

import com.backendseguros.gateway.filter.CorrelationIdWebFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

/**
 * Destino del filtro {@code CircuitBreaker} cuando el circuito hacia un servicio esta abierto o
 * la llamada agota el timeout. Responde siempre 503 con {@code Retry-After}, como define la
 * tabla de resiliencia de la propuesta de migracion (seccion 6.2,
 * doc/5. Microservicios/c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md).
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/identity")
    public ResponseEntity<ErrorResponse> identityUnavailable(ServerWebExchange exchange) {
        return noDisponible(exchange, "El servicio de identidad", "/fallback/identity");
    }

    @RequestMapping("/fallback/customer")
    public ResponseEntity<ErrorResponse> customerUnavailable(ServerWebExchange exchange) {
        return noDisponible(exchange, "El servicio de clientes", "/fallback/customer");
    }

    @RequestMapping("/fallback/claims")
    public ResponseEntity<ErrorResponse> claimsUnavailable(ServerWebExchange exchange) {
        return noDisponible(exchange, "El servicio de siniestros", "/fallback/claims");
    }

    @RequestMapping("/fallback/quotation")
    public ResponseEntity<ErrorResponse> quotationUnavailable(ServerWebExchange exchange) {
        return noDisponible(exchange, "El servicio de cotizaciones", "/fallback/quotation");
    }

    @RequestMapping("/fallback/policy")
    public ResponseEntity<ErrorResponse> policyUnavailable(ServerWebExchange exchange) {
        return noDisponible(exchange, "El servicio de pólizas", "/fallback/policy");
    }

    private ResponseEntity<ErrorResponse> noDisponible(
            ServerWebExchange exchange, String servicio, String path) {
        String correlationId = exchange.getAttribute(CorrelationIdWebFilter.CORRELATION_ID_ATTRIBUTE);
        ErrorResponse body =
                ErrorResponse.of(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "SERVICE_UNAVAILABLE",
                        servicio + " no esta respondiendo ahora mismo. Intenta de nuevo en unos"
                                + " segundos.",
                        path,
                        correlationId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "10")
                .body(body);
    }
}
