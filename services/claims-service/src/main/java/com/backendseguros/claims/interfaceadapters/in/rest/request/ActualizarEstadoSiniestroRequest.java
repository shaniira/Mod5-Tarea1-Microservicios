package com.backendseguros.claims.interfaceadapters.in.rest.request;

import com.backendseguros.claims.entities.enums.EstadoSiniestro;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoSiniestroRequest(@NotNull EstadoSiniestro estado) {}
