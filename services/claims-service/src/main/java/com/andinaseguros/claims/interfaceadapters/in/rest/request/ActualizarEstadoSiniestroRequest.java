package com.andinaseguros.claims.interfaceadapters.in.rest.request;

import com.andinaseguros.claims.entities.enums.EstadoSiniestro;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoSiniestroRequest(@NotNull EstadoSiniestro estado) {}
