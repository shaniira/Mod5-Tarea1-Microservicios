package com.andinaseguros.entities.valueobject;

import java.time.LocalDate;

public record PeriodoVigencia(LocalDate inicio, LocalDate fin) {
    public PeriodoVigencia {
        if (inicio == null || fin == null || !fin.isAfter(inicio))
            throw new IllegalArgumentException("Periodo de vigencia inválido");
    }

    public boolean contiene(LocalDate fecha) {
        return !fecha.isBefore(inicio) && !fecha.isAfter(fin);
    }
}
