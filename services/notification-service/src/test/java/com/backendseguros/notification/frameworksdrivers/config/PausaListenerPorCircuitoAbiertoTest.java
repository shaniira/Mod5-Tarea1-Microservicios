package com.backendseguros.notification.frameworksdrivers.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;

class PausaListenerPorCircuitoAbiertoTest {
    private final CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("whatsapp");
    private final MessageListenerContainer container = mock(MessageListenerContainer.class);
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    PausaListenerPorCircuitoAbiertoTest() {
        new PausaListenerPorCircuitoAbierto(circuitBreaker, () -> container, Runnable::run, meters);
    }

    @Test
    void alAbrirseElCircuitoSeDetieneElListener() {
        when(container.isRunning()).thenReturn(true);

        circuitBreaker.transitionToOpenState();

        verify(container).stop();
    }

    @Test
    void alPasarASemiabiertoSeReanudaElListener() {
        when(container.isRunning()).thenReturn(true);
        circuitBreaker.transitionToOpenState();
        when(container.isRunning()).thenReturn(false);

        circuitBreaker.transitionToHalfOpenState();

        verify(container).start();
    }

    @Test
    void laMetricaIndicaSiElListenerEstaPausado() {
        when(container.isRunning()).thenReturn(false);

        assertThat(meters.get("notification.listener.paused").gauge().value()).isEqualTo(1.0);
    }
}
