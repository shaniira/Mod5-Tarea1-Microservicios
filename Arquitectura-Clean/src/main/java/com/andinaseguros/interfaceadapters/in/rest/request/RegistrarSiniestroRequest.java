package com.andinaseguros.interfaceadapters.in.rest.request;

import com.andinaseguros.entities.enums.EstadoSiniestro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarSiniestroRequest(
        UUID polizaId,
        @NotNull LocalDate fecha,
        @NotBlank String tipo,
        @NotNull @Positive BigDecimal montoEstimado,
        boolean responsabilidadAsegurado,
        @NotBlank String gravedad,
        @NotNull EstadoSiniestro estado) {}
