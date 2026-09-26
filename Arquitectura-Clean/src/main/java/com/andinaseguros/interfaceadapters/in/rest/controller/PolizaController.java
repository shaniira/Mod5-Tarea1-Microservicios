package com.andinaseguros.interfaceadapters.in.rest.controller;

import com.andinaseguros.interfaceadapters.in.rest.request.EmitirPolizaRequest;
import static com.andinaseguros.interfaceadapters.in.rest.mapper.RestRequestMapper.toCore;
import com.andinaseguros.usecases.dto.Responses.PolizaResponse;
import com.andinaseguros.usecases.port.in.EmitirPolizaInputPort;
import com.andinaseguros.usecases.service.poliza.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/polizas")
public class PolizaController {
    private final EmitirPolizaInputPort emitirPolizaUseCase;
    private final ObtenerPolizaUseCase obtenerPolizaUseCase;
    private final ListarPolizasUseCase listarPolizasUseCase;

    public PolizaController(
            EmitirPolizaInputPort emitirPolizaUseCase,
            ObtenerPolizaUseCase obtenerPolizaUseCase,
            ListarPolizasUseCase listarPolizasUseCase) {
        this.emitirPolizaUseCase = emitirPolizaUseCase;
        this.obtenerPolizaUseCase = obtenerPolizaUseCase;
        this.listarPolizasUseCase = listarPolizasUseCase;
    }

    @PostMapping
    public ResponseEntity<PolizaResponse> emitir(@Valid @RequestBody EmitirPolizaRequest solicitud) {
        return ResponseEntity.status(201).body(emitirPolizaUseCase.execute(toCore(solicitud)));
    }

    @GetMapping
    public List<PolizaResponse> listar(
            @RequestParam(required = false) com.andinaseguros.entities.enums.EstadoPoliza estado) {
        return listarPolizasUseCase.execute(estado);
    }

    @GetMapping("/{id}")
    public PolizaResponse obtener(@PathVariable UUID id) {
        return obtenerPolizaUseCase.execute(id);
    }
}
