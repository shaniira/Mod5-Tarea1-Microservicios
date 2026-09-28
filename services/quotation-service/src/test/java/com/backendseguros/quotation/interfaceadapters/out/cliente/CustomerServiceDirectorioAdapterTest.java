package com.backendseguros.quotation.interfaceadapters.out.cliente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.backendseguros.quotation.entities.enums.TipoVehiculo;
import com.backendseguros.quotation.usecases.exception.ClientesNoDisponiblesException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Paso 5.3: lectura de refuerzo con reintento, circuit breaker y 503 controlado. */
class CustomerServiceDirectorioAdapterTest {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://customer-service:8080");
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
    private final CircuitBreaker circuitBreaker =
            CircuitBreaker.of(
                    "customer",
                    CircuitBreakerConfig.custom()
                            .slidingWindowSize(2)
                            .minimumNumberOfCalls(2)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofMinutes(1))
                            .recordExceptions(CustomerServiceDirectorioAdapter.CustomerServiceCaido.class)
                            .build());
    private final Retry retry =
            Retry.of(
                    "customer",
                    RetryConfig.custom()
                            .maxAttempts(2)
                            .waitDuration(Duration.ofMillis(1))
                            .retryExceptions(CustomerServiceDirectorioAdapter.CustomerServiceCaido.class)
                            .build());
    private final CustomerServiceDirectorioAdapter adapter =
            new CustomerServiceDirectorioAdapter(builder.build(), circuitBreaker, retry);
    private final UUID clienteId = UUID.randomUUID();

    @Test
    void traeElClienteConSuFechaDeNacimiento() {
        servidor.expect(requestTo("http://customer-service:8080/api/clientes/" + clienteId))
                .andRespond(
                        withSuccess(
                                "{\"id\":\"" + clienteId + "\",\"fechaNacimiento\":\"1990-01-15\",\"activo\":true,\"nombres\":\"Ana\"}",
                                MediaType.APPLICATION_JSON));

        var cliente = adapter.buscarCliente(clienteId);

        assertThat(cliente).isPresent();
        assertThat(cliente.get().fechaNacimiento()).isEqualTo(LocalDate.of(1990, 1, 15));
    }

    @Test
    void unVehiculoQueNoExisteEsVacioYNoCuentaComoFalla() {
        UUID vehiculoId = UUID.randomUUID();
        servidor.expect(requestTo("http://customer-service:8080/api/clientes/" + clienteId + "/vehiculos/" + vehiculoId))
                .andRespond(withResourceNotFound());

        assertThat(adapter.buscarVehiculo(clienteId, vehiculoId)).isEmpty();
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void traeElVehiculo() {
        UUID vehiculoId = UUID.randomUUID();
        servidor.expect(requestTo("http://customer-service:8080/api/clientes/" + clienteId + "/vehiculos/" + vehiculoId))
                .andRespond(
                        withSuccess(
                                "{\"id\":\"" + vehiculoId + "\",\"clienteId\":\"" + clienteId
                                        + "\",\"tipo\":\"CAMIONETA\",\"uso\":\"PARTICULAR\",\"anioFabricacion\":2022}",
                                MediaType.APPLICATION_JSON));

        assertThat(adapter.buscarVehiculo(clienteId, vehiculoId).get().tipo()).isEqualTo(TipoVehiculo.CAMIONETA);
    }

    @Test
    void conCustomerServiceCaidoReintentaUnaVezYRespondeNoDisponible() {
        servidor.expect(ExpectedCount.times(2), requestTo("http://customer-service:8080/api/clientes/" + clienteId))
                .andRespond(withServerError());

        assertThatThrownBy(() -> adapter.buscarCliente(clienteId)).isInstanceOf(ClientesNoDisponiblesException.class);
        servidor.verify();
    }

    @Test
    void conElCircuitoAbiertoNoSeLlamaACustomerService() {
        circuitBreaker.transitionToOpenState();

        assertThatThrownBy(() -> adapter.buscarCliente(clienteId)).isInstanceOf(ClientesNoDisponiblesException.class);
        servidor.verify();
    }
}
