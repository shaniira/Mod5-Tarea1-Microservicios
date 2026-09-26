package com.andinaseguros.interfaceadapters.in.rest.controller;

import com.andinaseguros.usecases.dto.Responses.MiCuentaResponse;
import com.andinaseguros.usecases.service.cliente.ObtenerMiCuentaUseCase;
import org.springframework.security.core.Authentication;
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
    public MiCuentaResponse miCuenta(Authentication authentication) {
        return obtenerMiCuenta.execute(authentication.getName());
    }
}
