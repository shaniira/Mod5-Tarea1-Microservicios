package com.backendseguros.gateway.web;

import java.time.Instant;

/** Forma unica de error para todas las respuestas que arma el Gateway (401, 503, etc). */
public record ErrorResponse(
        Instant timestamp, int status, String error, String message, String path, String correlationId) {

    public static ErrorResponse of(int status, String error, String message, String path, String correlationId) {
        return new ErrorResponse(Instant.now(), status, error, message, path, correlationId);
    }
}
