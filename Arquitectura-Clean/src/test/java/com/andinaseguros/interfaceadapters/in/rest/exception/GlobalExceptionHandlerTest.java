package com.andinaseguros.interfaceadapters.in.rest.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.andinaseguros.entities.exception.ReglaNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {
    @Test
    void invalidCredentialsReturnUnauthorizedInsteadOfUnprocessableEntity() {
        var request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        var response =
                new GlobalExceptionHandler()
                        .domain(
                                new ReglaNegocioException(
                                        "CREDENCIALES_INVALIDAS", "Credenciales inválidas"),
                                request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().status());
        assertEquals("CREDENCIALES_INVALIDAS", response.getBody().codigo());
    }

    @Test
    void googleTokenInvalidoReturnsUnauthorized() {
        var request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/auth/google");

        var response =
                new GlobalExceptionHandler()
                        .domain(
                                new ReglaNegocioException(
                                        "GOOGLE_TOKEN_INVALIDO",
                                        "No se pudo validar el token de Google"),
                                request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void clienteNoRegistradoReturnsForbidden() {
        var request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/auth/google");

        var response =
                new GlobalExceptionHandler()
                        .domain(
                                new ReglaNegocioException(
                                        "CLIENTE_NO_REGISTRADO",
                                        "Tu correo no está registrado como cliente"),
                                request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
