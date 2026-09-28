package com.backendseguros.policy.interfaceadapters.out.siniestros;

import com.backendseguros.policy.entities.model.SiniestroRef;
import com.backendseguros.policy.usecases.exception.SiniestrosNoDisponiblesException;
import com.backendseguros.policy.usecases.port.out.siniestros.HistorialSiniestrosPort;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Consulta síncrona a claims-service (GET /api/polizas/{id}/siniestros) al generar una póliza
 * renovada: confirma contra la fuente lo que la copia claim_ref puede no tener todavía. Timeout de
 * 2 s (en el RestClient), un reintento (es un GET) y circuit breaker "claims". Reenvía el token
 * del usuario: claims-service aplica sus propias reglas de acceso. Cualquier fallo termina en
 * {@link SiniestrosNoDisponiblesException} (503): nunca se asume "sin siniestros".
 */
public class ClaimsServiceHistorialAdapter implements HistorialSiniestrosPort {
    private static final Logger log = LoggerFactory.getLogger(ClaimsServiceHistorialAdapter.class);
    /** Estados cerrados en claims-service (igual que la carga inicial de claim_ref). */
    private static final Set<String> CERRADOS = Set.of("LIQUIDADO", "RECHAZADO");

    private final RestClient client;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public ClaimsServiceHistorialAdapter(RestClient client, CircuitBreaker circuitBreaker, Retry retry) {
        this.client = client;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
    }

    @Override
    public List<SiniestroRef> listarPorPoliza(UUID polizaId) {
        String token = tokenActual();
        Supplier<SiniestroDto[]> llamada = () -> get(polizaId, token);
        try {
            SiniestroDto[] siniestros =
                    CircuitBreaker.decorateSupplier(circuitBreaker, Retry.decorateSupplier(retry, llamada)).get();
            return Arrays.stream(siniestros == null ? new SiniestroDto[0] : siniestros)
                    .map(s -> new SiniestroRef(s.id(), s.polizaId(), !CERRADOS.contains(s.estado()), s.responsabilidadAsegurado()))
                    .toList();
        } catch (CallNotPermittedException abierto) {
            log.warn("claims-service no consultado: circuito abierto");
            throw new SiniestrosNoDisponiblesException(abierto);
        } catch (ClaimsServiceCaido | RespuestaNoEsperada error) {
            log.warn("claims-service no confirmó los siniestros de la póliza {}: {}", polizaId, error.getMessage());
            throw new SiniestrosNoDisponiblesException(error);
        }
    }

    private SiniestroDto[] get(UUID polizaId, String token) {
        try {
            return client.get()
                    .uri("/api/polizas/{id}/siniestros", polizaId)
                    .headers(h -> { if (token != null) h.set(HttpHeaders.AUTHORIZATION, "Bearer " + token); })
                    .retrieve()
                    .body(SiniestroDto[].class);
        } catch (HttpClientErrorException otro) {
            throw new RespuestaNoEsperada(otro);
        } catch (RestClientException caido) {
            throw new ClaimsServiceCaido(caido);
        }
    }

    private static String tokenActual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth instanceof JwtAuthenticationToken jwt ? jwt.getToken().getTokenValue() : null;
    }

    /** Sin respuesta, timeout o 5xx: cuenta para el circuito y se reintenta. */
    public static class ClaimsServiceCaido extends RuntimeException {
        ClaimsServiceCaido(Throwable causa) {
            super(causa.getMessage(), causa);
        }
    }

    /** 4xx: no se reintenta ni abre el circuito. */
    public static class RespuestaNoEsperada extends RuntimeException {
        RespuestaNoEsperada(Throwable causa) {
            super(causa.getMessage(), causa);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SiniestroDto(UUID id, UUID polizaId, String estado, boolean responsabilidadAsegurado) {}
}
