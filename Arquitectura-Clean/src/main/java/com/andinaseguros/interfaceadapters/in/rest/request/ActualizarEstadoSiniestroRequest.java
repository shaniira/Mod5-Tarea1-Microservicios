package com.andinaseguros.interfaceadapters.in.rest.request;

import com.andinaseguros.entities.enums.EstadoSiniestro;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoSiniestroRequest(@NotNull EstadoSiniestro estado) {}
