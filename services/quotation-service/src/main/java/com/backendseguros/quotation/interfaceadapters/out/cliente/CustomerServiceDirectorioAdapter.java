package com.backendseguros.quotation.interfaceadapters.out.cliente;

import com.backendseguros.quotation.entities.enums.TipoUso;
import com.backendseguros.quotation.entities.enums.TipoVehiculo;
import com.backendseguros.quotation.entities.model.ClienteRef;
import com.backendseguros.quotation.entities.model.VehiculoRef;
import com.backendseguros.quotation.usecases.exception.ClientesNoDisponiblesException;
import com.backendseguros.quotation.usecases.port.out.cliente.DirectorioClientesPort;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.time.LocalDate;
import java.util.Optional;
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
 * Lectura de refuerzo hacia customer-service (paso 5.3, sección 5.4 de la propuesta): solo cuando
 * la proyección no tiene el dato. Timeout de 2 s (en el RestClient), un reintento (son GET) y
 * circuit breaker "customer". Reenvía el token del usuario: customer-service aplica sus propias
 * reglas de acceso. Solo cuentan para el circuito las caídas (sin respuesta, timeout, 5xx).
 */
public class CustomerServiceDirectorioAdapter implements DirectorioClientesPort {
    private static final Logger log = LoggerFactory.getLogger(CustomerServiceDirectorioAdapter.class);

    private final RestClient client;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public CustomerServiceDirectorioAdapter(RestClient client, CircuitBreaker circuitBreaker, Retry retry) {
        this.client = client;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
    }

    @Override
    public Optional<ClienteRef> buscarCliente(UUID clienteId) {
        return consultar("/api/clientes/{id}", ClienteDto.class, clienteId)
                .map(c -> new ClienteRef(c.id(), c.fechaNacimiento(), c.activo()));
    }

    @Override
    public Optional<VehiculoRef> buscarVehiculo(UUID clienteId, UUID vehiculoId) {
        return consultar("/api/clientes/{id}/vehiculos/{vehiculoId}", VehiculoDto.class, clienteId, vehiculoId)
                .map(
                        v ->
                                new VehiculoRef(
                                        v.id(),
                                        v.clienteId(),
                                        TipoVehiculo.valueOf(v.tipo()),
                                        TipoUso.valueOf(v.uso()),
                                        v.anioFabricacion()));
    }

    private <T> Optional<T> consultar(String ruta, Class<T> tipo, Object... variables) {
        String token = tokenActual();
        Supplier<Optional<T>> llamada = () -> get(ruta, tipo, token, variables);
        try {
            return CircuitBreaker.decorateSupplier(circuitBreaker, Retry.decorateSupplier(retry, llamada)).get();
        } catch (CallNotPermittedException abierto) {
            log.warn("customer-service no consultado: circuito abierto");
            throw new ClientesNoDisponiblesException(abierto);
        } catch (CustomerServiceCaido | RespuestaNoEsperada error) {
            log.warn("customer-service no respondió a {}: {}", ruta, error.getMessage());
            throw new ClientesNoDisponiblesException(error);
        }
    }

    private <T> Optional<T> get(String ruta, Class<T> tipo, String token, Object... variables) {
        try {
            return Optional.ofNullable(
                    client.get()
                            .uri(ruta, variables)
                            .headers(h -> { if (token != null) h.set(HttpHeaders.AUTHORIZATION, "Bearer " + token); })
                            .retrieve()
                            .body(tipo));
        } catch (HttpClientErrorException.NotFound noExiste) {
            return Optional.empty();
        } catch (HttpClientErrorException otro) {
            throw new RespuestaNoEsperada(otro);
        } catch (RestClientException caido) {
            throw new CustomerServiceCaido(caido);
        }
    }

    private static String tokenActual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth instanceof JwtAuthenticationToken jwt ? jwt.getToken().getTokenValue() : null;
    }

    /** Sin respuesta, timeout o 5xx: cuenta para el circuito y se reintenta. */
    public static class CustomerServiceCaido extends RuntimeException {
        CustomerServiceCaido(Throwable causa) {
            super(causa.getMessage(), causa);
        }
    }

    /** 401, 403 u otro 4xx distinto de 404: no se reintenta ni abre el circuito. */
    public static class RespuestaNoEsperada extends RuntimeException {
        RespuestaNoEsperada(Throwable causa) {
            super(causa.getMessage(), causa);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ClienteDto(UUID id, LocalDate fechaNacimiento, boolean activo) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record VehiculoDto(UUID id, UUID clienteId, String tipo, String uso, int anioFabricacion) {}
}
