package com.backendseguros.notification.interfaceadapters.out.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.backendseguros.notification.entities.model.Notificacion;
import com.backendseguros.notification.usecases.exception.CanalNoDisponibleException;
import com.backendseguros.notification.usecases.exception.NotificacionRechazadaException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

class WhatsAppNotificationAdapterTest {
    private final WhatsAppClient client = mock(WhatsAppClient.class);
    private final CircuitBreaker circuitBreaker =
            CircuitBreaker.of(
                    "whatsapp",
                    CircuitBreakerConfig.custom()
                            .slidingWindowSize(4)
                            .minimumNumberOfCalls(4)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofMinutes(1))
                            .recordExceptions(WhatsAppTransitorioException.class)
                            .build());
    private final Retry retry =
            Retry.of(
                    "whatsapp",
                    RetryConfig.custom()
                            .maxAttempts(2)
                            .waitDuration(Duration.ofMillis(1))
                            .retryExceptions(WhatsAppTransitorioException.class)
                            .build());
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final WhatsAppNotificationAdapter adapter =
            new WhatsAppNotificationAdapter(client, circuitBreaker, retry, meters);
    private final Notificacion notificacion = new Notificacion("+51 921 175 206", "hola", Map.of());

    @Test
    void enviaYNormalizaElNumero() {
        when(client.enviarTexto(any())).thenReturn(new WhatsAppTextResponse(true, "ok", Map.of()));

        adapter.enviar(notificacion);

        verify(client).enviarTexto(new WhatsAppTextRequest("51921175206", "hola"));
        assertThat(meters.counter("notification.whatsapp.sent", "result", "success").count()).isEqualTo(1);
    }

    @Test
    void unaCaidaSeReintentaYTerminaComoCanalNoDisponible() {
        when(client.enviarTexto(any())).thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> adapter.enviar(notificacion))
                .isInstanceOf(CanalNoDisponibleException.class);
        verify(client, times(2)).enviarTexto(any());
    }

    @Test
    void conElCircuitoAbiertoNoSeLlamaAlProveedor() {
        when(client.enviarTexto(any())).thenThrow(new ResourceAccessException("timeout"));
        // 2 envíos x 2 intentos = 4 fallos registrados: el circuito se abre.
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> adapter.enviar(notificacion))
                    .isInstanceOf(CanalNoDisponibleException.class);
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        clearInvocations(client);

        assertThatThrownBy(() -> adapter.enviar(notificacion))
                .isInstanceOf(CanalNoDisponibleException.class)
                .hasMessageContaining("abierto");
        verifyNoInteractions(client);
    }

    @Test
    void unRechazoDelProveedorNoSeReintentaNiAbreElCircuito() {
        when(client.enviarTexto(any()))
                .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "bad", null, null, null));

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> adapter.enviar(notificacion))
                    .isInstanceOf(NotificacionRechazadaException.class);
        }
        verify(client, times(4)).enviarTexto(any());
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void respuestaSinExitoEsUnRechazo() {
        when(client.enviarTexto(any())).thenReturn(new WhatsAppTextResponse(false, "numero invalido", Map.of()));

        assertThatThrownBy(() -> adapter.enviar(notificacion))
                .isInstanceOf(NotificacionRechazadaException.class)
                .hasMessageContaining("numero invalido");
    }
}
