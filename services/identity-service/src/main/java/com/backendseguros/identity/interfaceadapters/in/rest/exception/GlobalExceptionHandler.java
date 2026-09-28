package com.backendseguros.identity.interfaceadapters.in.rest.exception;

import com.backendseguros.identity.entities.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.*;
import org.slf4j.*;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

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
                    case "CREDENCIALES_INVALIDAS",
                            "GOOGLE_TOKEN_INVALIDO",
                            "GOOGLE_EMAIL_NO_VERIFICADO",
                            "USUARIO_INACTIVO" -> HttpStatus.UNAUTHORIZED;
                    case "CLIENTE_NO_REGISTRADO", "ROL_NO_PERMITIDO" -> HttpStatus.FORBIDDEN;
                    case "FACEBOOK_CALLBACK_INVALIDO",
                            "FACEBOOK_IDENTIDAD_INVALIDA",
                            "FACEBOOK_AUTORIZACION_RECHAZADA",
                            "FACEBOOK_TICKET_INVALIDO" -> HttpStatus.BAD_REQUEST;
                    case "VINCULACION_REQUIERE_CONFIRMACION", "USUARIO_DUPLICADO" -> HttpStatus.CONFLICT;
                    case "FACEBOOK_NO_DISPONIBLE", "GOOGLE_NO_DISPONIBLE" -> HttpStatus.SERVICE_UNAVAILABLE;
                    default -> HttpStatus.UNPROCESSABLE_ENTITY;
                };

        return ResponseEntity.status(status)
                .body(
                        new ApiError(
                                OffsetDateTime.now(),
                                status.value(),
                                exception.getCodigo(),
                                exception.getMessage(),
                                request.getRequestURI(),
                                Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception
                .getBindingResult()
                .getFieldErrors()
                .forEach(
                        fieldError ->
                                fieldErrors.put(
                                        fieldError.getField(), fieldError.getDefaultMessage()));

        return ResponseEntity.badRequest()
                .body(
                        new ApiError(
                                OffsetDateTime.now(),
                                400,
                                "DATOS_INVALIDOS",
                                "La solicitud contiene datos inválidos",
                                request.getRequestURI(),
                                fieldErrors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> accessDenied(
            AccessDeniedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        new ApiError(
                                OffsetDateTime.now(),
                                403,
                                "ACCESO_DENEGADO",
                                "No tienes permisos para realizar esta acción",
                                request.getRequestURI(),
                                Map.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> illegal(
            IllegalArgumentException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(
                        new ApiError(
                                OffsetDateTime.now(),
                                400,
                                "ARGUMENTO_INVALIDO",
                                exception.getMessage(),
                                request.getRequestURI(),
                                Map.of()));
    }

    /**
     * Fase 7: un id que no es UUID, un parámetro de otro tipo o un JSON mal formado son errores del
     * cliente (400). Antes caían en el 500 genérico, igual que en el monolito.
     */
    @ExceptionHandler({TypeMismatchException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> malFormada(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(
                        new ApiError(
                                OffsetDateTime.now(),
                                400,
                                "SOLICITUD_MAL_FORMADA",
                                "La solicitud tiene un formato inválido",
                                request.getRequestURI(),
                                Map.of()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> generic(Exception exception, HttpServletRequest request) {
        if (exception instanceof ErrorResponse respuesta) {
            // Errores propios de Spring MVC con su código: ruta inexistente (404), método no
            // permitido (405), tipo de contenido no soportado (415)...
            int status = respuesta.getStatusCode().value();
            String codigo = status == 404 ? "RECURSO_NO_ENCONTRADO" : "SOLICITUD_NO_SOPORTADA";
            return ResponseEntity.status(status)
                    .body(
                            new ApiError(
                                    OffsetDateTime.now(),
                                    status,
                                    codigo,
                                    respuesta.getBody().getTitle(),
                                    request.getRequestURI(),
                                    Map.of()));
        }
        log.error("Error no controlado en {}", request.getRequestURI(), exception);

        return ResponseEntity.status(500)
                .body(
                        new ApiError(
                                OffsetDateTime.now(),
                                500,
                                "ERROR_INTERNO",
                                "Ocurrió un error interno",
                                request.getRequestURI(),
                                Map.of()));
    }
}
