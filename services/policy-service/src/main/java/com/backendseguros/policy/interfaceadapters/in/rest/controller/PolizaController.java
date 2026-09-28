package com.backendseguros.policy.interfaceadapters.in.rest.controller;

import com.backendseguros.policy.entities.enums.EstadoPoliza;
import com.backendseguros.policy.interfaceadapters.in.rest.request.EmitirPolizaRequest;
import com.backendseguros.policy.interfaceadapters.in.rest.security.Roles;
import com.backendseguros.policy.usecases.dto.EmitirPolizaRequestModel;
import com.backendseguros.policy.usecases.dto.Responses.PolizaResponse;
import com.backendseguros.policy.usecases.service.poliza.EmitirPolizaUseCase;
import com.backendseguros.policy.usecases.service.poliza.ListarPolizasUseCase;
import com.backendseguros.policy.usecases.service.poliza.ObtenerPolizaUseCase;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Mismas rutas, reglas de acceso y respuestas que /api/polizas del monolito. */
@RestController
@RequestMapping("/api/polizas")
public class PolizaController {
    private final EmitirPolizaUseCase emitir;
    private final ObtenerPolizaUseCase obtener;
    private final ListarPolizasUseCase listar;

    public PolizaController(EmitirPolizaUseCase emitir, ObtenerPolizaUseCase obtener, ListarPolizasUseCase listar) {
        this.emitir = emitir;
        this.obtener = obtener;
        this.listar = listar;
    }

    @PostMapping
    @PreAuthorize(Roles.OPERACION)
    public ResponseEntity<PolizaResponse> emitir(@Valid @RequestBody EmitirPolizaRequest solicitud) {
        return ResponseEntity.status(201)
                .body(emitir.execute(new EmitirPolizaRequestModel(solicitud.cotizacionId(), solicitud.inicioVigencia())));
    }

    @GetMapping
    @PreAuthorize(Roles.PERSONAL)
    public List<PolizaResponse> listar(@RequestParam(required = false) EstadoPoliza estado) {
        return listar.execute(estado);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.PERSONAL + " or @acceso.esDuenoDePoliza(#id)")
    public PolizaResponse obtener(@PathVariable UUID id) {
        return obtener.execute(id);
    }
}
