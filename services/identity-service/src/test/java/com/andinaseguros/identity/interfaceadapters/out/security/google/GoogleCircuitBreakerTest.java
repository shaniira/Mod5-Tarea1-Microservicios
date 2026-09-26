package com.andinaseguros.identity.interfaceadapters.out.security.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.andinaseguros.identity.usecases.exception.ProveedorIdentidadNoDisponibleException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class GoogleCircuitBreakerTest {
    private final JwtDecoder decoder = mock(JwtDecoder.class);
    private final CircuitBreaker circuitBreaker =
            CircuitBreaker.of(
                    "google",
                    CircuitBreakerConfig.custom()
                            .slidingWindowSize(4)
                            .minimumNumberOfCalls(4)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofMinutes(1))
                            .recordExceptions(GoogleIdentityVerifierAdapter.GoogleSinRespuesta.class)
                            .build());
    private final GoogleIdentityVerifierAdapter adapter =
            new GoogleIdentityVerifierAdapter(
                    decoder, "client", "https://accounts.google.com", circuitBreaker, Bulkhead.ofDefaults("google"));

    @Test
    void siGoogleNoRespondeElCircuitoSeAbreYNoSeVuelveALlamar() {
        when(decoder.decode(anyString()))
                .thenThrow(new JwtException("Couldn't retrieve remote JWK set: Read timed out"));

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> adapter.verificar("token"))
                    .isInstanceOf(ProveedorIdentidadNoDisponibleException.class);
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        clearInvocations(decoder);

        assertThatThrownBy(() -> adapter.verificar("token"))
                .isInstanceOf(ProveedorIdentidadNoDisponibleException.class);
        verifyNoInteractions(decoder);
    }

    @Test
    void conElBulkheadLlenoSeRespondeNoDisponibleSinLlamarAGoogle() {
        Bulkhead lleno =
                Bulkhead.of(
                        "google",
                        io.github.resilience4j.bulkhead.BulkheadConfig.custom()
                                .maxConcurrentCalls(1)
                                .maxWaitDuration(Duration.ZERO)
                                .build());
        lleno.tryAcquirePermission(); // otra petición ocupa el único cupo
        var conCupoLleno =
                new GoogleIdentityVerifierAdapter(
                        decoder, "client", "https://accounts.google.com", circuitBreaker, lleno);

        assertThatThrownBy(() -> conCupoLleno.verificar("token"))
                .isInstanceOf(ProveedorIdentidadNoDisponibleException.class);
        verifyNoInteractions(decoder);
    }

    @Test
    void losTokensInvalidosNoAbrenElCircuito() {
        when(decoder.decode(anyString())).thenThrow(new BadJwtException("Signed JWT rejected"));

        for (int i = 0; i < 6; i++) {
            assertThatThrownBy(() -> adapter.verificar("token"))
                    .isInstanceOf(BadJwtException.class);
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
