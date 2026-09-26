package com.andinaseguros.entities.service;

import com.andinaseguros.entities.enums.EstadoRenovacion;

public class PoliticaVariacionPrima {
    public String explicar(EstadoRenovacion decision, int siniestrosResponsables) {
        return switch (decision) {
            case AUTOMATICA ->
                    "Sin siniestros con responsabilidad: renovación automática sin recargo";
            case REQUIERE_RECALCULO ->
                    siniestrosResponsables + " siniestro(s) responsable(s): se recalculó la prima";
            case EVALUACION_MANUAL ->
                    siniestrosResponsables + " siniestros responsables: requiere evaluación manual";
            default -> "Evaluación por historial de siniestros";
        };
    }
}
