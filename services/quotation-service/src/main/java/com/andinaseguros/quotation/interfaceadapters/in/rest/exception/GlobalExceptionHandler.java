package com.andinaseguros.quotation.interfaceadapters.in.rest.exception;

import com.andinaseguros.quotation.entities.exception.DomainException;
import com.andinaseguros.quotation.usecases.exception.ClientesNoDisponiblesException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Mismo formato de error (ApiError) y mismos códigos HTTP que el monolito. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ApiError(
            OffsetDateTime timestamp,
            int status,
            String codigo,
            String mensaje,
            String path,
            Map<String, String> campos) {}

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> domain(DomainException exception, HttpServletRequest request) {
        HttpStatus status =
                switch (exception.getCodigo()) {
                    case "RECURSO_NO_ENCONTRADO" -> HttpStatus.NOT_FOUND;
                    // Paso 5.3: customer-service no respondió y el dato no estaba en la proyección.
                    case ClientesNoDisponiblesException.CODIGO -> HttpStatus.SERVICE_UNAVAILABLE;
                    default -> HttpStatus.UNPROCESSABLE_ENTITY;
                };
        var respuesta = ResponseEntity.status(status);
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            respuesta.header("Retry-After", "5");
        }
        return respuesta.body(
                error(status.value(), exception.getCodigo(), exception.getMessage(), request, Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception
                .getBindingResult()
                .getFieldErrors()
                .forEach(fieldError -> fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(error(400, "DATOS_INVALIDOS", "La solicitud contiene datos inválidos", request, fieldErrors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> accessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        error(
                                403,
                                "ACCESO_DENEGADO",
                                "No tienes permisos para realizar esta acción",
                                request,
                                Map.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> illegal(IllegalArgumentException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(error(400, "ARGUMENTO_INVALIDO", exception.getMessage(), request, Map.of()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> generic(Exception exception, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), exception);
        return ResponseEntity.status(500)
                .body(error(500, "ERROR_INTERNO", "Ocurrió un error interno", request, Map.of()));
    }

    private static ApiError error(
            int status, String codigo, String mensaje, HttpServletRequest request, Map<String, String> campos) {
        return new ApiError(OffsetDateTime.now(), status, codigo, mensaje, request.getRequestURI(), campos);
    }
}
