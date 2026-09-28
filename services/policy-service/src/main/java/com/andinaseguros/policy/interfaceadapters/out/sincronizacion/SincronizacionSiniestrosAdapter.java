package com.andinaseguros.policy.interfaceadapters.out.sincronizacion;

import com.andinaseguros.policy.usecases.port.out.repository.SincronizacionSiniestrosPort;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * claim_ref está al día si (1) ya tuvo la carga inicial y (2) la cola policy.claim.events no tiene
 * eventos de siniestros esperando. Si alguno no se cumple, o no se puede comprobar (RabbitMQ no
 * responde), se considera atrasada: la renovación responde "siniestros pendientes" y se reintenta.
 *
 * <p>Fase 7: "esperando" incluye los eventos que el consumidor ya recibió y aún aplica (antes solo
 * se contaban los listos en la cola, y con prefetch alto podía haber cientos sin contar). Límites
 * que quedan: un evento que sigue en el Outbox de claims (hasta ~1 s, el intervalo del relay) y,
 * con varias réplicas, el que esté aplicando otra réplica (milisegundos).
 */
public class SincronizacionSiniestrosAdapter implements SincronizacionSiniestrosPort {
    private static final Logger log = LoggerFactory.getLogger(SincronizacionSiniestrosAdapter.class);

    private final BooleanSupplier cargaInicialHecha;
    private final LongSupplier eventosEsperando;

    public SincronizacionSiniestrosAdapter(BooleanSupplier cargaInicialHecha, LongSupplier eventosEsperando) {
        this.cargaInicialHecha = cargaInicialHecha;
        this.eventosEsperando = eventosEsperando;
    }

    @Override
    public boolean estaAlDia() {
        if (!cargaInicialHecha.getAsBoolean()) {
            log.warn("claim_ref sin carga inicial: no se evalúan renovaciones");
            return false;
        }
        try {
            long pendientes = eventosEsperando.getAsLong();
            if (pendientes > 0) {
                log.warn("claim_ref atrasada: {} evento(s) de siniestros por aplicar", pendientes);
            }
            return pendientes == 0;
        } catch (RuntimeException sinRespuesta) {
            log.warn("No se pudo comprobar la cola de siniestros ({}); se trata como atrasada", sinRespuesta.getMessage());
            return false;
        }
    }
}
