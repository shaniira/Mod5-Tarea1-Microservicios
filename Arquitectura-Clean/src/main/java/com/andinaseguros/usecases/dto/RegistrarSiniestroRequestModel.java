package com.andinaseguros.usecases.dto;

import com.andinaseguros.entities.enums.EstadoSiniestro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarSiniestroRequestModel(
        UUID polizaId,
        LocalDate fecha,
        String tipo,
        BigDecimal montoEstimado,
        boolean responsabilidadAsegurado,
        String gravedad,
        EstadoSiniestro estado) {}
