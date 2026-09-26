package com.andinaseguros.identity.interfaceadapters.in.rest.controller;

import com.andinaseguros.identity.usecases.service.auth.GestionarSesionesUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Logout real (riesgo S8) y activación/desactivación de usuarios con revocación inmediata. */
@RestController
@RequestMapping("/api/auth")
public class SesionController {
    private final GestionarSesionesUseCase sesiones;

    public SesionController(GestionarSesionesUseCase sesiones) {
        this.sesiones = sesiones;
    }

    /** Revoca el token con el que se llama: deja de valer en el gateway y en identity. */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(JwtAuthenticationToken authentication) {
        sesiones.cerrarSesion(authentication.getToken().getId(), authentication.getToken().getExpiresAt());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/usuarios/{username}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cambiarEstado(
            @PathVariable String username, @Valid @RequestBody EstadoUsuarioRequest solicitud) {
        sesiones.cambiarEstado(username, solicitud.activo());
        return ResponseEntity.noContent().build();
    }

    public record EstadoUsuarioRequest(@NotNull Boolean activo) {}
}
