package com.andinaseguros.quotation.interfaceadapters.in.rest.controller;

import static com.andinaseguros.quotation.interfaceadapters.in.rest.mapper.RestRequestMapper.toCore;

import com.andinaseguros.quotation.entities.enums.EstadoCotizacion;
import com.andinaseguros.quotation.interfaceadapters.in.rest.request.CrearCotizacionRequest;
import com.andinaseguros.quotation.interfaceadapters.in.rest.security.Roles;
import com.andinaseguros.quotation.usecases.dto.Responses.CotizacionResponse;
import com.andinaseguros.quotation.usecases.port.in.CrearCotizacionInputPort;
import com.andinaseguros.quotation.usecases.service.cotizacion.AceptarCotizacionUseCase;
import com.andinaseguros.quotation.usecases.service.cotizacion.ListarCotizacionesPendientesEmisionUseCase;
import com.andinaseguros.quotation.usecases.service.cotizacion.ListarCotizacionesUseCase;
import com.andinaseguros.quotation.usecases.service.cotizacion.ObtenerCotizacionUseCase;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Mismas rutas, reglas de acceso y respuestas que /api/cotizaciones del monolito. */
@RestController
@RequestMapping("/api/cotizaciones")
public class CotizacionController {
    private final CrearCotizacionInputPort crearCotizacion;
    private final ObtenerCotizacionUseCase obtenerCotizacion;
    private final AceptarCotizacionUseCase aceptarCotizacion;
    private final ListarCotizacionesUseCase listarCotizaciones;
    private final ListarCotizacionesPendientesEmisionUseCase listarPendientes;

    public CotizacionController(
            CrearCotizacionInputPort crearCotizacion,
            ObtenerCotizacionUseCase obtenerCotizacion,
            AceptarCotizacionUseCase aceptarCotizacion,
            ListarCotizacionesUseCase listarCotizaciones,
            ListarCotizacionesPendientesEmisionUseCase listarPendientes) {
        this.crearCotizacion = crearCotizacion;
        this.obtenerCotizacion = obtenerCotizacion;
        this.aceptarCotizacion = aceptarCotizacion;
        this.listarCotizaciones = listarCotizaciones;
        this.listarPendientes = listarPendientes;
    }

    @GetMapping
    @PreAuthorize(Roles.OPERACION)
    public List<CotizacionResponse> listar(@RequestParam(required = false) EstadoCotizacion estado) {
        return listarCotizaciones.execute(estado);
    }

    @GetMapping("/pendientes-emision")
    @PreAuthorize(Roles.OPERACION)
    public List<CotizacionResponse> pendientesEmision() {
        return listarPendientes.execute();
    }

    @PostMapping
    @PreAuthorize(Roles.OPERACION)
    public ResponseEntity<CotizacionResponse> crear(@Valid @RequestBody CrearCotizacionRequest solicitud) {
        return ResponseEntity.status(201).body(crearCotizacion.execute(toCore(solicitud)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.OPERACION + " or @acceso.esDuenoDeCotizacion(#id)")
    public CotizacionResponse obtener(@PathVariable UUID id) {
        return obtenerCotizacion.execute(id);
    }

    @PatchMapping("/{id}/aceptar")
    @PreAuthorize(Roles.OPERACION)
    public CotizacionResponse aceptar(@PathVariable UUID id) {
        return aceptarCotizacion.execute(id);
    }
}
