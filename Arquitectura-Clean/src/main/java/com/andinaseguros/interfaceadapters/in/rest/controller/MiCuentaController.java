package com.andinaseguros.interfaceadapters.in.rest.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.andinaseguros.interfaceadapters.in.rest.security.Roles;
import com.andinaseguros.usecases.dto.Responses.MiCuentaResponse;
import com.andinaseguros.usecases.service.cliente.ObtenerMiCuentaUseCase;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mi-cuenta")
public class MiCuentaController {
    private final ObtenerMiCuentaUseCase obtenerMiCuenta;

    public MiCuentaController(ObtenerMiCuentaUseCase obtenerMiCuenta) {
        this.obtenerMiCuenta = obtenerMiCuenta;
    }

    @GetMapping
    @PreAuthorize(Roles.CLIENTE)
    public MiCuentaResponse miCuenta(Authentication authentication) {
        return obtenerMiCuenta.execute(authentication.getName(), customerId(authentication));
    }

    private UUID customerId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) {
            String valor = jwt.getToken().getClaimAsString("customerId");
            return valor == null || valor.isBlank() ? null : UUID.fromString(valor);
        }
        return null;
    }
}
