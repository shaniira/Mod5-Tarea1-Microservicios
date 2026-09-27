package com.andinaseguros.quotation.interfaceadapters.in.rest.controller;

import static com.andinaseguros.quotation.interfaceadapters.in.rest.mapper.RestRequestMapper.toCore;

import com.andinaseguros.quotation.interfaceadapters.in.rest.request.CrearTablaRequest;
import com.andinaseguros.quotation.interfaceadapters.in.rest.security.Roles;
import com.andinaseguros.quotation.usecases.dto.Responses.TablaDetalleResponse;
import com.andinaseguros.quotation.usecases.dto.Responses.TablaResponse;
import com.andinaseguros.quotation.usecases.service.tarifa.CrearTablaTarifariaUseCase;
import com.andinaseguros.quotation.usecases.service.tarifa.ListarTablasTarifariasUseCase;
import com.andinaseguros.quotation.usecases.service.tarifa.ObtenerTablaTarifariaUseCase;
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
import org.springframework.web.bind.annotation.RestController;

/** Mismas rutas, reglas de acceso y respuestas que /api/tablas-tarifarias del monolito. */
@RestController
@RequestMapping("/api/tablas-tarifarias")
public class TablaTarifariaController {
    private final CrearTablaTarifariaUseCase crearTabla;
    private final ListarTablasTarifariasUseCase listarTablas;
    private final ObtenerTablaTarifariaUseCase obtenerTabla;

    public TablaTarifariaController(
            CrearTablaTarifariaUseCase crearTabla,
            ListarTablasTarifariasUseCase listarTablas,
            ObtenerTablaTarifariaUseCase obtenerTabla) {
        this.crearTabla = crearTabla;
        this.listarTablas = listarTablas;
        this.obtenerTabla = obtenerTabla;
    }

    @PostMapping
    @PreAuthorize(Roles.TARIFAS)
    public ResponseEntity<TablaResponse> crear(@Valid @RequestBody CrearTablaRequest solicitud) {
        return ResponseEntity.status(201).body(crearTabla.execute(toCore(solicitud)));
    }

    @GetMapping
    @PreAuthorize(Roles.PERSONAL)
    public List<TablaResponse> listar() {
        return listarTablas.execute();
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.PERSONAL)
    public TablaDetalleResponse obtener(@PathVariable UUID id) {
        return obtenerTabla.execute(id);
    }
}
