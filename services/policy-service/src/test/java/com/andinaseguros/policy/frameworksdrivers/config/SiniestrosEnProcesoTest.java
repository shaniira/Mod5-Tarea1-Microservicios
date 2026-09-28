package com.andinaseguros.policy.frameworksdrivers.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicLong;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/** Fase 7: un evento de siniestros entregado y sin aplicar cuenta como pendiente para claim_ref. */
class SiniestrosEnProcesoTest {
    private final SiniestrosEnProceso enProceso = new SiniestrosEnProceso("policy.claim.events");

    @Test
    void cuentaElEventoDeSiniestrosMientrasSeAplicaAunqueFalle() throws Throwable {
        AtomicLong visto = new AtomicLong(-1);
        MethodInvocation invocacion = invocacion("policy.claim.events");
        when(invocacion.proceed()).thenAnswer(i -> {
            visto.set(enProceso.getAsLong());
            throw new IllegalStateException("fallo al aplicar");
        });

        assertThatThrownBy(() -> enProceso.invoke(invocacion)).isInstanceOf(IllegalStateException.class);

        assertThat(visto.get()).isEqualTo(1);
        assertThat(enProceso.getAsLong()).isZero();
    }

    @Test
    void noCuentaLosEventosDeOtrasColas() throws Throwable {
        AtomicLong visto = new AtomicLong(-1);
        MethodInvocation invocacion = invocacion("policy.quote.events");
        when(invocacion.proceed()).thenAnswer(i -> {
            visto.set(enProceso.getAsLong());
            return null;
        });

        enProceso.invoke(invocacion);

        assertThat(visto.get()).isZero();
    }

    private static MethodInvocation invocacion(String cola) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setConsumerQueue(cola);
        MethodInvocation invocacion = mock(MethodInvocation.class);
        when(invocacion.getArguments()).thenReturn(new Object[] {null, new Message(new byte[0], propiedades)});
        return invocacion;
    }
}
