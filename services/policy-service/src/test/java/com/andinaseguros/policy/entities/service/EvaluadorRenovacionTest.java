package com.andinaseguros.policy.entities.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.andinaseguros.policy.entities.enums.EstadoRenovacion;
import com.andinaseguros.policy.entities.exception.ReglaNegocioException;
import com.andinaseguros.policy.entities.model.SiniestroRef;
import com.andinaseguros.policy.entities.valueobject.Dinero;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** La prueba del monolito (4 responsables = evaluación manual) y el resto de la regla. */
class EvaluadorRenovacionTest {
    private final EvaluadorRenovacion evaluador = new EvaluadorRenovacion();
    private final UUID poliza = UUID.randomUUID();

    @Test
    void enviaAEvaluacionManualConCuatroSiniestros() {
        List<SiniestroRef> s = new ArrayList<>();
        for (int i = 0; i < 4; i++) s.add(cerrado(true));
        assertEquals(EstadoRenovacion.EVALUACION_MANUAL, evaluador.evaluar(s));
    }

    @Test
    void conUnSiniestroAbiertoNoSeRenueva() {
        assertThrows(ReglaNegocioException.class, () -> evaluador.evaluar(List.of(cerrado(false), abierto())));
    }

    @Test
    void recalculaConResponsablesYRenuevaSolaSinEllos() {
        assertEquals(EstadoRenovacion.REQUIERE_RECALCULO, evaluador.evaluar(List.of(cerrado(true), cerrado(false))));
        assertEquals(EstadoRenovacion.AUTOMATICA, evaluador.evaluar(List.of(cerrado(false))));
        assertEquals(EstadoRenovacion.AUTOMATICA, evaluador.evaluar(List.of()));
    }

    @Test
    void laPrimaDeRenovacionEsLaDelMonolito() {
        // 2 responsables: factor 1 + 2^1.35 * 0.08 (mismo cálculo que CalculadorPrimaRenovacion del monolito).
        var r = new CalculadorPrimaRenovacion().calcular(Dinero.soles(new BigDecimal("1000")), 2);
        assertEquals(new BigDecimal("1203.93"), r.nuevaPrima().valor());
        assertEquals(new BigDecimal("20.3900"), r.porcentajeVariacion());
    }

    private SiniestroRef cerrado(boolean responsable) {
        return new SiniestroRef(UUID.randomUUID(), poliza, false, responsable);
    }

    private SiniestroRef abierto() {
        return new SiniestroRef(UUID.randomUUID(), poliza, true, false);
    }
}
