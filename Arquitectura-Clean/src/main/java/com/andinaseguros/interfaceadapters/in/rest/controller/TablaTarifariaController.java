package com.andinaseguros.interfaceadapters.in.rest.controller;

import com.andinaseguros.interfaceadapters.in.rest.request.CrearTablaRequest;
import static com.andinaseguros.interfaceadapters.in.rest.mapper.RestRequestMapper.toCore;
import com.andinaseguros.usecases.dto.Responses.*;
import com.andinaseguros.usecases.service.tarifa.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tablas-tarifarias")
public class TablaTarifariaController {
    private final CrearTablaTarifariaUseCase crearTablaUseCase;
    private final ListarTablasTarifariasUseCase listarTablasUseCase;
    private final ObtenerTablaTarifariaUseCase obtenerTablaUseCase;

    public TablaTarifariaController(
            CrearTablaTarifariaUseCase crearTablaUseCase,
            ListarTablasTarifariasUseCase listarTablasUseCase,
            ObtenerTablaTarifariaUseCase obtenerTablaUseCase) {
        this.crearTablaUseCase = crearTablaUseCase;
        this.listarTablasUseCase = listarTablasUseCase;
        this.obtenerTablaUseCase = obtenerTablaUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACTUARIO')")
    public ResponseEntity<TablaResponse> crear(@Valid @RequestBody CrearTablaRequest solicitud) {
        return ResponseEntity.status(201).body(crearTablaUseCase.execute(toCore(solicitud)));
    }

    @GetMapping
    public List<TablaResponse> listar() {
        return listarTablasUseCase.execute();
    }

    @GetMapping("/{id}")
    public TablaDetalleResponse obtener(@PathVariable UUID id) {
        return obtenerTablaUseCase.execute(id);
    }
}
