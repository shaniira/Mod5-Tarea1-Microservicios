package com.backendseguros.customer.interfaceadapters.out.external.jsonpe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.backendseguros.customer.usecases.exception.VehicleProviderAuthenticationException;
import com.backendseguros.customer.usecases.exception.VehicleProviderTimeoutException;
import com.backendseguros.customer.usecases.exception.VehicleProviderUnavailableException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Paso 3.2: reintento, circuit breaker y bulkhead alrededor de JSON.pe. */
class JsonPeResilienciaTest {
    private final JsonPeClient client = mock(JsonPeClient.class);
    private final CircuitBreaker circuitBreaker =
            CircuitBreaker.of(
                    "jsonpe",
                    CircuitBreakerConfig.custom()
                            .slidingWindowSize(4)
                            .minimumNumberOfCalls(4)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofMinutes(1))
                            .recordExceptions(
                                    VehicleProviderUnavailableException.class,
                                    VehicleProviderTimeoutException.class)
                            .build());
    private final Retry retry =
            Retry.of(
                    "jsonpe",
                    RetryConfig.custom()
                            .maxAttempts(2)
                            .waitDuration(Duration.ofMillis(1))
                            .retryExceptions(
                                    VehicleProviderUnavailableException.class,
                                    VehicleProviderTimeoutException.class)
                            .build());

    private JsonPeVehicleInformationAdapter adapter(Bulkhead bulkhead) {
        return new JsonPeVehicleInformationAdapter(client, new JsonPeVehicleMapper(), circuitBreaker, retry, bulkhead);
    }

    @Test
    void reintentaUnaVezUnaCaidaBreveYDevuelveLosDatos() {
        when(client.consultar("B6U170"))
                .thenThrow(new VehicleProviderTimeoutException("JSONPE_TIMEOUT", new RuntimeException()))
                .thenReturn(
                        new JsonPePlateResponse(
                                true, "ok", new JsonPeVehicleData("B6U170", "RENAULT", "LOGAN", null, null, null, "VIN")));

        var resultado = adapter(Bulkhead.ofDefaults("jsonpe")).consultarPorPlaca("B6U170");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().marca()).isEqualTo("RENAULT");
        verify(client, times(2)).consultar("B6U170");
    }

    @Test
    void conJsonPeCaidoElCircuitoSeAbreYNoSeVuelveALlamar() {
        when(client.consultar(anyString()))
                .thenThrow(new VehicleProviderUnavailableException("JSONPE_UNAVAILABLE", new RuntimeException()));
        var adapter = adapter(Bulkhead.ofDefaults("jsonpe"));

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> adapter.consultarPorPlaca("B6U170"))
                    .isInstanceOf(VehicleProviderUnavailableException.class);
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        clearInvocations(client);

        assertThatThrownBy(() -> adapter.consultarPorPlaca("B6U170"))
                .isInstanceOf(VehicleProviderUnavailableException.class);
        verifyNoInteractions(client);
    }

    @Test
    void unTokenRechazadoNoSeReintentaNiAbreElCircuito() {
        when(client.consultar(anyString()))
                .thenThrow(new VehicleProviderAuthenticationException("JSONPE_UNAUTHORIZED", new RuntimeException()));
        var adapter = adapter(Bulkhead.ofDefaults("jsonpe"));

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> adapter.consultarPorPlaca("B6U170"))
                    .isInstanceOf(VehicleProviderAuthenticationException.class);
        }
        verify(client, times(5)).consultar("B6U170");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void conElBulkheadLlenoSeRespondeNoDisponibleSinLlamarAJsonPe() {
        Bulkhead lleno =
                Bulkhead.of("jsonpe", BulkheadConfig.custom().maxConcurrentCalls(1).maxWaitDuration(Duration.ZERO).build());
        lleno.tryAcquirePermission(); // otra consulta ocupa el único cupo

        assertThatThrownBy(() -> adapter(lleno).consultarPorPlaca("B6U170"))
                .isInstanceOf(VehicleProviderUnavailableException.class);
        verifyNoInteractions(client);
    }
}
