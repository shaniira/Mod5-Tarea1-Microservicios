package com.andinaseguros.notification.frameworksdrivers.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.andinaseguros.notification.usecases.exception.CanalNoDisponibleException;
import com.andinaseguros.notification.usecases.exception.ContactoNoDisponibleException;
import com.andinaseguros.notification.usecases.exception.EventoInvalidoException;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.retry.RetryContext;
import org.springframework.retry.context.RetryContextSupport;
import org.springframework.retry.policy.SimpleRetryPolicy;

class ManejoFallosMensajesTest {
    private final Message message = new Message(new byte[0], new MessageProperties());
    private final ManejoFallosMensajes.Recuperador recuperador = new ManejoFallosMensajes.Recuperador();

    @Test
    void conElCanalCaidoElMensajeVuelveALaCola() {
        Throwable error = envuelto(new CanalNoDisponibleException("abierto", null));

        assertThatThrownBy(() -> recuperador.recover(message, error))
                .isInstanceOf(ImmediateRequeueAmqpException.class);
    }

    @Test
    void cualquierOtroFalloVaALaDlq() {
        Throwable error = envuelto(new ContactoNoDisponibleException("sin contacto"));

        assertThatThrownBy(() -> recuperador.recover(message, error))
                .hasCauseInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void soloSeReintentanLosFallosQuePuedenResolverseSolos() {
        SimpleRetryPolicy politica = ManejoFallosMensajes.politicaDeReintentos(3);

        assertThat(puedeReintentar(politica, new ContactoNoDisponibleException("lag"))).isTrue();
        assertThat(puedeReintentar(politica, new EventoInvalidoException("mal"))).isFalse();
        assertThat(puedeReintentar(politica, new CanalNoDisponibleException("caido", null))).isFalse();
    }

    private static boolean puedeReintentar(SimpleRetryPolicy politica, Exception causa) {
        RetryContext context = new RetryContextSupport(null);
        ((RetryContextSupport) context).registerThrowable(envuelto(causa));
        return politica.canRetry(context);
    }

    private static Throwable envuelto(Exception causa) {
        return new ListenerExecutionFailedException("fallo", causa);
    }
}
