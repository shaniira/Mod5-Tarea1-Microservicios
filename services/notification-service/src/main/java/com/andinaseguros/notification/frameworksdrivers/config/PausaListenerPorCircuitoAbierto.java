package com.andinaseguros.notification.frameworksdrivers.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;

/**
 * Resiliencia en mensajería (sección 6.3 de la propuesta): en un consumidor, "abrir el circuito"
 * significa dejar de consumir. Cuando el circuit breaker de WhatsApp se abre, se detiene el
 * listener de pólizas y los mensajes esperan en su cola (no se queman reintentos ni se llenan la
 * DLQ). Cuando pasa a semiabierto, se reanuda y los primeros mensajes hacen de prueba: si fallan,
 * el circuito se vuelve a abrir y el listener se detiene otra vez.
 *
 * <p>El listener se detiene y arranca en otro hilo: la transición ocurre dentro del hilo que está
 * procesando un mensaje, y ese hilo no puede esperar a que su propio contenedor se detenga.
 */
public class PausaListenerPorCircuitoAbierto {
    private static final Logger log = LoggerFactory.getLogger(PausaListenerPorCircuitoAbierto.class);

    private final Supplier<MessageListenerContainer> contenedor;
    private final Executor executor;

    public PausaListenerPorCircuitoAbierto(
            CircuitBreaker circuitBreaker,
            Supplier<MessageListenerContainer> contenedor,
            Executor executor,
            MeterRegistry meterRegistry) {
        this.contenedor = contenedor;
        this.executor = executor;
        circuitBreaker.getEventPublisher().onStateTransition(this::alCambiarEstado);
        Gauge.builder("notification.listener.paused", this, p -> p.estaPausado() ? 1 : 0)
                .description("1 si el consumo de policy.issued esta pausado por el circuit breaker")
                .tag("listener", "policyIssued")
                .register(meterRegistry);
    }

    void alCambiarEstado(CircuitBreakerOnStateTransitionEvent event) {
        switch (event.getStateTransition().getToState()) {
            case OPEN, FORCED_OPEN -> executor.execute(this::pausar);
            case HALF_OPEN, CLOSED -> executor.execute(this::reanudar);
            default -> {}
        }
    }

    boolean estaPausado() {
        MessageListenerContainer container = contenedor.get();
        return container != null && !container.isRunning();
    }

    private void pausar() {
        MessageListenerContainer container = contenedor.get();
        if (container != null && container.isRunning()) {
            log.warn("Circuit breaker de WhatsApp abierto: se pausa el consumo de policy.issued");
            container.stop();
        }
    }

    private void reanudar() {
        MessageListenerContainer container = contenedor.get();
        if (container != null && !container.isRunning()) {
            log.info("Circuit breaker de WhatsApp semiabierto/cerrado: se reanuda el consumo");
            container.start();
        }
    }
}
