package com.andinaseguros.claims.interfaceadapters.in.rest.controller;

import com.andinaseguros.claims.interfaceadapters.in.rest.security.Roles;
import com.andinaseguros.claims.usecases.dto.Responses.ReenvioEventosResponse;
import com.andinaseguros.claims.usecases.service.siniestro.PublicarSiniestrosExistentesUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Backfill de claim.registered.v1 (solo ADMIN). Ruta nueva: no existía en el monolito. */
@RestController
public class SiniestroEventosController {
    private final PublicarSiniestrosExistentesUseCase publicarSiniestros;

    public SiniestroEventosController(PublicarSiniestrosExistentesUseCase publicarSiniestros) {
        this.publicarSiniestros = publicarSiniestros;
    }

    @PostMapping("/api/siniestros/eventos/reenvio")
    @PreAuthorize(Roles.ADMIN)
    public ResponseEntity<ReenvioEventosResponse> reenviar() {
        return ResponseEntity.accepted().body(publicarSiniestros.execute());
    }
}
