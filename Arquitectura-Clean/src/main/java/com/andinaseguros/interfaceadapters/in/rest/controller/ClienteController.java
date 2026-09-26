package com.andinaseguros.interfaceadapters.in.rest.controller;

import com.andinaseguros.interfaceadapters.in.rest.request.*;
import static com.andinaseguros.interfaceadapters.in.rest.mapper.RestRequestMapper.toCore;
import com.andinaseguros.usecases.dto.Responses.*;
import com.andinaseguros.usecases.port.in.RegistrarClienteUseCase;
import com.andinaseguros.usecases.port.in.RegistrarVehiculoUseCase;
import com.andinaseguros.usecases.service.cliente.*;
import com.andinaseguros.usecases.service.vehiculo.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
    private final RegistrarClienteUseCase crearClienteUseCase;
    private final ListarClientesUseCase listarClientesUseCase;
    private final ObtenerClienteUseCase obtenerClienteUseCase;
    private final RegistrarVehiculoUseCase crearVehiculo;
    private final ListarVehiculosClienteUseCase listarVehiculos;

    public ClienteController(
            RegistrarClienteUseCase crearClienteUseCase,
            ListarClientesUseCase listarClientesUseCase,
            ObtenerClienteUseCase obtenerClienteUseCase,
            RegistrarVehiculoUseCase crearVehiculoUseCase,
            ListarVehiculosClienteUseCase listarVehiculosClienteUseCase) {
        this.crearClienteUseCase = crearClienteUseCase;
        this.listarClientesUseCase = listarClientesUseCase;
        this.obtenerClienteUseCase = obtenerClienteUseCase;
        this.crearVehiculo = crearVehiculoUseCase;
        this.listarVehiculos = listarVehiculosClienteUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','AGENTE')")
    ResponseEntity<ClienteResponse> crear(@Valid @RequestBody CrearClienteRequest solicitud) {
        return ResponseEntity.status(201).body(crearClienteUseCase.execute(toCore(solicitud)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','AGENTE')")
    List<ClienteResponse> listar() {
        return listarClientesUseCase.execute();
    }

    @GetMapping("/{id}")
    ClienteResponse obtener(@PathVariable UUID id) {
        return obtenerClienteUseCase.execute(id);
    }

    @PostMapping("/{id}/vehiculos")
    ResponseEntity<VehiculoResponse> vehiculo(
            @PathVariable UUID id, @Valid @RequestBody CrearVehiculoRequest solicitud) {
        return ResponseEntity.status(201)
                .body(
                        crearVehiculo.execute(
                                new com.andinaseguros.usecases.dto.CrearVehiculoRequestModel(
                                        id,
                                        solicitud.placa(),
                                        solicitud.marca(),
                                        solicitud.modelo(),
                                        solicitud.anioFabricacion(),
                                        solicitud.tipo(),
                                        solicitud.uso(),
                                        solicitud.zonaCirculacion())));
    }

    @GetMapping("/{id}/vehiculos")
    List<VehiculoResponse> vehiculos(@PathVariable UUID id) {
        return listarVehiculos.execute(id);
    }
}
