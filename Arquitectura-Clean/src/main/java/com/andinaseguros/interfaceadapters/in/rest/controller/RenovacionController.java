package com.andinaseguros.interfaceadapters.in.rest.controller;

import com.andinaseguros.usecases.dto.Responses.*;
import com.andinaseguros.usecases.service.renovacion.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/renovaciones")
public class RenovacionController {
    private final ListarRenovacionesUseCase listarRenovacionesUseCase;
    private final ObtenerRenovacionUseCase obtenerRenovacionUseCase;
    private final ListarHistorialRenovacionesUseCase listarHistorialUseCase;
    private final EvaluarRenovacionUseCase evaluarRenovacionUseCase;
    private final AprobarRenovacionUseCase aprobarRenovacionUseCase;
    private final RechazarRenovacionUseCase rechazarRenovacionUseCase;
    private final GenerarPolizaRenovadaUseCase generarPolizaRenovadaUseCase;

    public RenovacionController(
            ListarRenovacionesUseCase listarRenovacionesUseCase,
            ObtenerRenovacionUseCase obtenerRenovacionUseCase,
            ListarHistorialRenovacionesUseCase listarHistorialUseCase,
            EvaluarRenovacionUseCase evaluarRenovacionUseCase,
            AprobarRenovacionUseCase aprobarRenovacionUseCase,
            RechazarRenovacionUseCase rechazarRenovacionUseCase,
            GenerarPolizaRenovadaUseCase generarPolizaRenovadaUseCase) {
        this.listarRenovacionesUseCase = listarRenovacionesUseCase;
        this.obtenerRenovacionUseCase = obtenerRenovacionUseCase;
        this.listarHistorialUseCase = listarHistorialUseCase;
        this.evaluarRenovacionUseCase = evaluarRenovacionUseCase;
        this.aprobarRenovacionUseCase = aprobarRenovacionUseCase;
        this.rechazarRenovacionUseCase = rechazarRenovacionUseCase;
        this.generarPolizaRenovadaUseCase = generarPolizaRenovadaUseCase;
    }

    @GetMapping
    public List<RenovacionResponse> listar() {
        return listarRenovacionesUseCase.execute();
    }

    @GetMapping("/{id}")
    public RenovacionResponse obtener(@PathVariable UUID id) {
        return obtenerRenovacionUseCase.execute(id);
    }

    @GetMapping("/poliza/{id}/historial")
    public List<RenovacionResponse> historial(@PathVariable UUID id) {
        return listarHistorialUseCase.execute(id);
    }

    @PostMapping("/poliza/{id}/evaluar")
    public ResponseEntity<RenovacionResponse> evaluar(@PathVariable UUID id) {
        return ResponseEntity.status(201).body(evaluarRenovacionUseCase.execute(id));
    }

    @PatchMapping("/{id}/aprobar")
    public RenovacionResponse aprobar(@PathVariable UUID id) {
        return aprobarRenovacionUseCase.execute(id);
    }

    @PatchMapping("/{id}/rechazar")
    public RenovacionResponse rechazar(@PathVariable UUID id) {
        return rechazarRenovacionUseCase.execute(id);
    }

    @PostMapping("/{id}/generar-poliza")
    public ResponseEntity<PolizaResponse> generar(@PathVariable UUID id) {
        return ResponseEntity.status(201).body(generarPolizaRenovadaUseCase.execute(id));
    }
}
