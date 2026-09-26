package com.andinaseguros.entities.service;

import static org.junit.jupiter.api.Assertions.*;

import com.andinaseguros.entities.enums.*;
import com.andinaseguros.entities.model.Siniestro;
import com.andinaseguros.entities.valueobject.Dinero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;

class EvaluadorRenovacionTest {
    @Test
    void enviaAEvaluacionManualConCuatroSiniestros() {
        UUID p = UUID.randomUUID();
        List<Siniestro> s = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            s.add(
                    new Siniestro(
                            UUID.randomUUID(),
                            p,
                            LocalDate.now(),
                            "CHOQUE",
                            Dinero.soles(BigDecimal.TEN),
                            true,
                            "ALTA",
                            EstadoSiniestro.LIQUIDADO));
        assertEquals(EstadoRenovacion.EVALUACION_MANUAL, new EvaluadorRenovacion().evaluar(s));
    }
}
