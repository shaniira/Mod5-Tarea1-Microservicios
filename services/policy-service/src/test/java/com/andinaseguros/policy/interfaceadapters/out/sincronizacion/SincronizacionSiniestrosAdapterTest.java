package com.andinaseguros.policy.interfaceadapters.out.sincronizacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Paso 6.5: claim_ref solo está al día con carga inicial y sin eventos de siniestros esperando. */
class SincronizacionSiniestrosAdapterTest {

    @Test
    void conCargaInicialYColaVaciaEstaAlDia() {
        assertThat(new SincronizacionSiniestrosAdapter(() -> true, () -> 0).estaAlDia()).isTrue();
    }

    @Test
    void conEventosDeSiniestrosEsperandoEstaAtrasada() {
        assertThat(new SincronizacionSiniestrosAdapter(() -> true, () -> 3).estaAlDia()).isFalse();
    }

    @Test
    void sinCargaInicialEstaAtrasada() {
        assertThat(new SincronizacionSiniestrosAdapter(() -> false, () -> 0).estaAlDia()).isFalse();
    }

    @Test
    void siNoSePuedeComprobarLaColaSeTrataComoAtrasada() {
        var adapter =
                new SincronizacionSiniestrosAdapter(
                        () -> true,
                        () -> {
                            throw new IllegalStateException("RabbitMQ no responde");
                        });
        assertThat(adapter.estaAlDia()).isFalse();
    }
}
