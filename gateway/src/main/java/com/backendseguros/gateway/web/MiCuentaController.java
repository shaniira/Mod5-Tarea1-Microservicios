package com.backendseguros.gateway.web;

import com.backendseguros.gateway.filter.CorrelationIdWebFilter;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * "Mi cuenta" como composición en el gateway (pasos 3.7 y 6.9): el cliente sale de customer-service
 * y sus pólizas con renovaciones de policy-service, en paralelo y con el token del usuario (cada
 * servicio aplica sus reglas). Mismo formato que respondía el monolito ({@code cliente},
 * {@code polizas}); si un servicio no responde a tiempo, esa sección llega vacía y se nombra en
 * {@code seccionesNoDisponibles} (respuesta parcial, sección 5.4 de la propuesta).
 */
@RestController
public class MiCuentaController {
    private static final Logger log = LoggerFactory.getLogger(MiCuentaController.class);
    static final Duration TIMEOUT = Duration.ofSeconds(3);
    static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final WebClient customer;
    private final WebClient policy;

    public MiCuentaController(
            WebClient.Builder builder,
            @Value("${app.services.customer-url}") String customerUrl,
            @Value("${app.services.policy-url}") String policyUrl) {
        this.customer = builder.clone().baseUrl(customerUrl).build();
        this.policy = builder.clone().baseUrl(policyUrl).build();
    }

    @GetMapping("/api/mi-cuenta")
    public Mono<ResponseEntity<Map<String, Object>>> miCuenta(
            @AuthenticationPrincipal Jwt jwt, ServerWebExchange exchange) {
        if (!"CLIENTE".equals(jwt.getClaimAsString("rol"))) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", 403, "codigo", "ACCESO_DENEGADO",
                    "mensaje", "No tienes permisos para realizar esta acción")));
        }
        String customerId = jwt.getClaimAsString("customerId");
        if (customerId == null || customerId.isBlank()) {
            // Usuario CLIENTE cuyo correo todavía no corresponde a un cliente registrado.
            return Mono.just(ResponseEntity.ok(respuesta(null, List.of(), List.of())));
        }
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String correlationId = exchange.getAttribute(CorrelationIdWebFilter.CORRELATION_ID_ATTRIBUTE);

        Mono<Parte> cliente = parte(customer, "/api/clientes/" + customerId, "cliente", authorization, correlationId);
        Mono<Parte> polizas = parte(policy, "/api/mi-cuenta/polizas", "polizas", authorization, correlationId);

        return Mono.zip(cliente, polizas)
                .map(t -> {
                    List<String> faltan = new ArrayList<>();
                    if (!t.getT1().ok()) faltan.add("cliente");
                    if (!t.getT2().ok()) faltan.add("polizas");
                    return ResponseEntity.ok(respuesta(t.getT1().valor(), t.getT2().ok() ? t.getT2().valor() : List.of(), faltan));
                });
    }

    private Mono<Parte> parte(WebClient cliente, String ruta, String seccion, String authorization, String correlationId) {
        return cliente.get()
                .uri(ruta)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header(CORRELATION_HEADER, correlationId == null ? "" : correlationId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(TIMEOUT)
                .map(valor -> new Parte(true, valor))
                .onErrorResume(error -> {
                    log.warn("Mi cuenta: la sección {} no respondió ({})", seccion, error.toString());
                    return Mono.just(new Parte(false, null));
                });
    }

    private static Map<String, Object> respuesta(Object cliente, Object polizas, List<String> faltan) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("cliente", cliente);
        cuerpo.put("polizas", polizas);
        if (!faltan.isEmpty()) {
            cuerpo.put("seccionesNoDisponibles", faltan);
        }
        return cuerpo;
    }

    private record Parte(boolean ok, Object valor) {}
}
