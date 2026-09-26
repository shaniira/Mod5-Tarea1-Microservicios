package com.andinaseguros.entities.service;

import com.andinaseguros.entities.enums.*;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.model.Siniestro;
import java.math.BigDecimal;
import java.util.List;

public class EvaluadorRenovacion {
    public EstadoRenovacion evaluar(List<Siniestro> siniestros) {
        boolean pendientes =
                siniestros.stream()
                        .anyMatch(
                                s ->
                                        s.estado() != EstadoSiniestro.LIQUIDADO
                                                && s.estado() != EstadoSiniestro.RECHAZADO);
        if (pendientes)
            throw new ReglaNegocioException(
                    "SINIESTROS_PENDIENTES", "No se puede renovar con siniestros pendientes");
        long responsables = siniestros.stream().filter(Siniestro::responsabilidadAsegurado).count();
        if (responsables > 3) return EstadoRenovacion.EVALUACION_MANUAL;
        if (responsables > 0) return EstadoRenovacion.REQUIERE_RECALCULO;
        return EstadoRenovacion.AUTOMATICA;
    }

    public BigDecimal factorNoLineal(int siniestrosResponsables) {
        return BigDecimal.ONE.add(
                BigDecimal.valueOf(Math.pow(siniestrosResponsables, 1.35))
                        .multiply(new BigDecimal("0.08")));
    }
}
