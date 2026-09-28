package com.andinaseguros.policy.frameworksdrivers.config;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.amqp.core.Message;

/**
 * Cuenta los eventos de siniestros que esta réplica ya recibió de policy.claim.events pero todavía
 * no terminó de aplicar (fase 7). La cola solo informa los mensajes "listos"; uno entregado al
 * consumidor y sin confirmar no aparece ahí, y la renovación lo daría por aplicado.
 *
 * <p>Va primero en la cadena de advice del contenedor, antes de los reintentos: el evento cuenta
 * como "en proceso" durante todos sus intentos y las esperas entre ellos. Con prefetch 1 (ver
 * application.yml) cada réplica tiene como mucho un mensaje sin confirmar, y es este.
 */
public class SiniestrosEnProceso implements MethodInterceptor, LongSupplier {
    private final String colaSiniestros;
    private final AtomicLong enProceso = new AtomicLong();

    public SiniestrosEnProceso(String colaSiniestros) {
        this.colaSiniestros = colaSiniestros;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (!esDeSiniestros(invocation.getArguments())) {
            return invocation.proceed();
        }
        enProceso.incrementAndGet();
        try {
            return invocation.proceed();
        } finally {
            enProceso.decrementAndGet();
        }
    }

    @Override
    public long getAsLong() {
        return enProceso.get();
    }

    private boolean esDeSiniestros(Object[] argumentos) {
        for (Object argumento : argumentos) {
            if (argumento instanceof Message mensaje) {
                return colaSiniestros.equals(mensaje.getMessageProperties().getConsumerQueue());
            }
        }
        return false;
    }
}
