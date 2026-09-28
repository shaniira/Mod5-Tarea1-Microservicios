package com.backendseguros.notification.interfaceadapters.out.whatsapp;

import com.backendseguros.notification.entities.model.Notificacion;
import com.backendseguros.notification.usecases.exception.CanalNoDisponibleException;
import com.backendseguros.notification.usecases.exception.NotificacionRechazadaException;
import com.backendseguros.notification.usecases.port.out.NotificacionPort;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Envía el WhatsApp con timeout (en el RestClient), reintentos con espera exponencial y circuit
 * breaker. Clasifica cada fallo en transitorio (el canal está caído: {@link
 * CanalNoDisponibleException}) o definitivo (el proveedor rechazó el mensaje: {@link
 * NotificacionRechazadaException}).
 */
public class WhatsAppNotificationAdapter implements NotificacionPort {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificationAdapter.class);

    private final WhatsAppClient client;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final MeterRegistry meterRegistry;

    public WhatsAppNotificationAdapter(
            WhatsAppClient client,
            CircuitBreaker circuitBreaker,
            Retry retry,
            MeterRegistry meterRegistry) {
        this.client = client;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void enviar(Notificacion notificacion) {
        String number = normalizarNumero(notificacion.destinatario());
        Supplier<WhatsAppTextResponse> llamada =
                Retry.decorateSupplier(
                        retry,
                        CircuitBreaker.decorateSupplier(
                                circuitBreaker,
                                () -> invocar(new WhatsAppTextRequest(number, notificacion.contenido()))));
        WhatsAppTextResponse response;
        try {
            response = llamada.get();
        } catch (CallNotPermittedException abierto) {
            contar("unavailable");
            throw new CanalNoDisponibleException("Circuit breaker de WhatsApp abierto", abierto);
        } catch (WhatsAppTransitorioException caido) {
            contar("unavailable");
            throw new CanalNoDisponibleException("WhatsApp no disponible: " + caido.getMessage(), caido);
        } catch (NotificacionRechazadaException rechazo) {
            contar("rejected");
            throw rechazo;
        }

        if (response == null || !response.success()) {
            contar("rejected");
            throw new NotificacionRechazadaException(
                    "JSON.pe rechazo el mensaje: "
                            + (response == null ? "respuesta vacia" : response.message()));
        }
        contar("success");
        log.info("WhatsApp enviado correctamente a {}", enmascarar(number));
    }

    private WhatsAppTextResponse invocar(WhatsAppTextRequest request) {
        try {
            return client.enviarTexto(request);
        } catch (HttpClientErrorException error) {
            if (error.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)
                    || error.getStatusCode().isSameCodeAs(HttpStatus.REQUEST_TIMEOUT)) {
                throw new WhatsAppTransitorioException("HTTP " + error.getStatusCode().value(), error);
            }
            throw new NotificacionRechazadaException(
                    "JSON.pe respondio HTTP " + error.getStatusCode().value(), error);
        } catch (HttpServerErrorException error) {
            throw new WhatsAppTransitorioException("HTTP " + error.getStatusCode().value(), error);
        } catch (ResourceAccessException error) {
            throw new WhatsAppTransitorioException("sin respuesta: " + error.getMessage(), error);
        }
    }

    private void contar(String resultado) {
        meterRegistry.counter("notification.whatsapp.sent", "result", resultado).increment();
    }

    private String normalizarNumero(String phone) {
        String number = phone == null ? "" : phone.replaceAll("[^0-9]", "");
        if (number.isBlank()) {
            throw new NotificacionRechazadaException("El telefono de destino es obligatorio");
        }
        return number;
    }

    private String enmascarar(String number) {
        int visibleDigits = Math.min(4, number.length());
        return "***" + number.substring(number.length() - visibleDigits);
    }
}
