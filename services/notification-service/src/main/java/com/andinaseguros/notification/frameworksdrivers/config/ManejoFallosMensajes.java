package com.andinaseguros.notification.frameworksdrivers.config;

import com.andinaseguros.notification.usecases.exception.CanalNoDisponibleException;
import com.andinaseguros.notification.usecases.exception.EventoInvalidoException;
import com.andinaseguros.notification.usecases.exception.NotificacionRechazadaException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.retry.policy.SimpleRetryPolicy;

/**
 * Qué pasa con un mensaje que falla:
 *
 * <ul>
 *   <li>Canal caído ({@link CanalNoDisponibleException}): no se reintenta aquí (el adaptador de
 *       WhatsApp ya reintentó) y <b>vuelve a la cola</b>; el circuit breaker pausa el listener hasta
 *       que WhatsApp se recupere. Así el mensaje no termina en la DLQ por una caída del proveedor.
 *   <li>Mensaje inválido o rechazado por el proveedor: no se reintenta y va directo a la DLQ.
 *   <li>Cualquier otro fallo (Mongo lento, contacto que aún no llega a la proyección): se reintenta
 *       con espera exponencial y, si sigue fallando, va a la DLQ.
 * </ul>
 */
final class ManejoFallosMensajes {
    private static final Logger log = LoggerFactory.getLogger(ManejoFallosMensajes.class);

    private ManejoFallosMensajes() {}

    static SimpleRetryPolicy politicaDeReintentos(int maxAttempts) {
        return new SimpleRetryPolicy(
                maxAttempts,
                Map.of(
                        CanalNoDisponibleException.class, false,
                        NotificacionRechazadaException.class, false,
                        EventoInvalidoException.class, false,
                        MessageConversionException.class, false),
                true,
                true);
    }

    static final class Recuperador implements MessageRecoverer {
        private final RejectAndDontRequeueRecoverer aDeadLetter = new RejectAndDontRequeueRecoverer();

        @Override
        public void recover(Message message, Throwable cause) {
            if (tieneCausa(cause, CanalNoDisponibleException.class)) {
                log.warn(
                        "Canal no disponible; el mensaje {} vuelve a la cola: {}",
                        message.getMessageProperties().getMessageId(),
                        causaRaiz(cause).getMessage());
                throw new ImmediateRequeueAmqpException("Canal de notificacion no disponible", cause);
            }
            log.error(
                    "Mensaje {} enviado a la DLQ: {}",
                    message.getMessageProperties().getMessageId(),
                    causaRaiz(cause).getMessage());
            aDeadLetter.recover(message, cause);
        }
    }

    static boolean tieneCausa(Throwable error, Class<? extends Throwable> tipo) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (tipo.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    private static Throwable causaRaiz(Throwable error) {
        Throwable t = error;
        while (t.getCause() != null && t.getCause() != t) {
            if (t instanceof CanalNoDisponibleException
                    || t instanceof NotificacionRechazadaException
                    || t instanceof EventoInvalidoException) {
                return t;
            }
            t = t.getCause();
        }
        return t;
    }
}
