package com.andinaseguros.interfaceadapters.in.rest.controller;

import com.andinaseguros.interfaceadapters.in.rest.request.MfaCodeRequest;
import com.andinaseguros.usecases.dto.Responses.MfaSetupResponse;
import com.andinaseguros.usecases.dto.Responses.MfaStatusResponse;
import com.andinaseguros.usecases.service.mfa.*;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mfa")
public class MfaController {
    private final ConfigurarMfaUseCase configurar;
    private final ActivarMfaUseCase activar;
    private final DesactivarMfaUseCase desactivar;
    private final ObtenerEstadoMfaUseCase estado;

    public MfaController(ConfigurarMfaUseCase configurar, ActivarMfaUseCase activar, DesactivarMfaUseCase desactivar, ObtenerEstadoMfaUseCase estado) {
        this.configurar = configurar; this.activar = activar; this.desactivar = desactivar; this.estado = estado;
    }

    @PostMapping("/configurar")
    public MfaSetupResponse configurar(Principal principal) { return configurar.execute(principal.getName()); }

    @PostMapping("/activar")
    public ResponseEntity<Void> activar(Principal principal, @Valid @RequestBody MfaCodeRequest request) {
        activar.execute(principal.getName(), request.codigo()); return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> desactivar(Principal principal, @Valid @RequestBody MfaCodeRequest request) {
        desactivar.execute(principal.getName(), request.codigo()); return ResponseEntity.noContent().build();
    }

    @GetMapping("/estado")
    public MfaStatusResponse estado(Principal principal) { return estado.execute(principal.getName()); }
}
