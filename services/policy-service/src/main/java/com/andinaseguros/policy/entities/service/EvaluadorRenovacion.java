package com.andinaseguros.policy.entities.service;

import com.andinaseguros.policy.entities.enums.EstadoRenovacion;
import com.andinaseguros.policy.entities.exception.ReglaNegocioException;
import com.andinaseguros.policy.entities.model.SiniestroRef;
import java.util.List;

/**
 * Misma regla que el monolito: con siniestros pendientes no se renueva; más de 3 responsables pide
 * evaluación manual, 1 a 3 recalcula la prima y ninguno renueva automáticamente. Recibe las
 * referencias de claim_ref en lugar de los siniestros completos (ahora son de claims-service).
 */
public class EvaluadorRenovacion {
    public EstadoRenovacion evaluar(List<SiniestroRef> siniestros) {
        if (siniestros.stream().anyMatch(SiniestroRef::abierto))
            throw new ReglaNegocioException(
                    "SINIESTROS_PENDIENTES", "No se puede renovar con siniestros pendientes");
        long responsables = siniestros.stream().filter(SiniestroRef::responsabilidadAsegurado).count();
        if (responsables > 3) return EstadoRenovacion.EVALUACION_MANUAL;
        if (responsables > 0) return EstadoRenovacion.REQUIERE_RECALCULO;
        return EstadoRenovacion.AUTOMATICA;
    }
}
