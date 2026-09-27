package com.andinaseguros.customer.interfaceadapters.in.rest.controller;

import com.andinaseguros.customer.interfaceadapters.in.rest.request.ActualizarContactoClienteRequest;
import com.andinaseguros.customer.interfaceadapters.in.rest.request.CrearClienteRequest;
import com.andinaseguros.customer.interfaceadapters.in.rest.request.CrearVehiculoRequest;
import com.andinaseguros.customer.interfaceadapters.in.rest.security.Roles;
import com.andinaseguros.customer.usecases.dto.ActualizarContactoClienteRequestModel;
import com.andinaseguros.customer.usecases.dto.CrearClienteRequestModel;
import com.andinaseguros.customer.usecases.dto.CrearVehiculoRequestModel;
import com.andinaseguros.customer.usecases.dto.Responses.ClienteResponse;
import com.andinaseguros.customer.usecases.dto.Responses.ReenvioEventosResponse;
import com.andinaseguros.customer.usecases.dto.Responses.VehiculoResponse;
import com.andinaseguros.customer.usecases.port.in.RegistrarClienteUseCase;
import com.andinaseguros.customer.usecases.port.in.RegistrarVehiculoUseCase;
import com.andinaseguros.customer.usecases.service.cliente.ActualizarContactoClienteUseCase;
import com.andinaseguros.customer.usecases.service.cliente.ListarClientesUseCase;
import com.andinaseguros.customer.usecases.service.cliente.ObtenerClienteUseCase;
import com.andinaseguros.customer.usecases.service.cliente.PublicarClientesExistentesUseCase;
import com.andinaseguros.customer.usecases.service.vehiculo.ListarVehiculosClienteUseCase;
import com.andinaseguros.customer.usecases.service.vehiculo.ObtenerVehiculoClienteUseCase;
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
import org.springframework.web.bind.annotation.RestController;

/** Mismas rutas, reglas de acceso y respuestas que /api/clientes del monolito (paso 3.1). */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
    private final RegistrarClienteUseCase crearCliente;
    private final ListarClientesUseCase listarClientes;
    private final ObtenerClienteUseCase obtenerCliente;
    private final RegistrarVehiculoUseCase crearVehiculo;
    private final ListarVehiculosClienteUseCase listarVehiculos;
    private final ObtenerVehiculoClienteUseCase obtenerVehiculo;
    private final ActualizarContactoClienteUseCase actualizarContacto;
    private final PublicarClientesExistentesUseCase publicarClientesExistentes;

    public ClienteController(
            RegistrarClienteUseCase crearCliente,
            ListarClientesUseCase listarClientes,
            ObtenerClienteUseCase obtenerCliente,
            RegistrarVehiculoUseCase crearVehiculo,
            ListarVehiculosClienteUseCase listarVehiculos,
            ObtenerVehiculoClienteUseCase obtenerVehiculo,
            ActualizarContactoClienteUseCase actualizarContacto,
            PublicarClientesExistentesUseCase publicarClientesExistentes) {
        this.crearCliente = crearCliente;
        this.listarClientes = listarClientes;
        this.obtenerCliente = obtenerCliente;
        this.crearVehiculo = crearVehiculo;
        this.listarVehiculos = listarVehiculos;
        this.obtenerVehiculo = obtenerVehiculo;
        this.actualizarContacto = actualizarContacto;
        this.publicarClientesExistentes = publicarClientesExistentes;
    }

    @PostMapping
    @PreAuthorize(Roles.OPERACION)
    ResponseEntity<ClienteResponse> crear(@Valid @RequestBody CrearClienteRequest solicitud) {
        return ResponseEntity.status(201)
                .body(
                        crearCliente.execute(
                                new CrearClienteRequestModel(
                                        solicitud.tipoDocumento(),
                                        solicitud.numeroDocumento(),
                                        solicitud.nombres(),
                                        solicitud.apellidos(),
                                        solicitud.fechaNacimiento(),
                                        solicitud.correo(),
                                        solicitud.telefono())));
    }

    @PatchMapping("/{id}/contacto")
    @PreAuthorize(Roles.OPERACION)
    ClienteResponse actualizarContacto(
            @PathVariable UUID id, @Valid @RequestBody ActualizarContactoClienteRequest solicitud) {
        return actualizarContacto.execute(
                new ActualizarContactoClienteRequestModel(
                        id, solicitud.correo(), solicitud.telefono()));
    }

    /**
     * Backfill (paso 3.4): vuelve a publicar customer.registered.v1 por cada cliente y
     * vehicle.registered.v1 por cada vehículo. Puebla o reconstruye las proyecciones de
     * notification, identity y el backend. Conserva el campo clientesPublicados del monolito.
     */
    @PostMapping("/eventos/reenvio")
    @PreAuthorize(Roles.ADMIN)
    ResponseEntity<ReenvioEventosResponse> reenviarEventos() {
        return ResponseEntity.accepted().body(publicarClientesExistentes.execute());
    }

    @GetMapping
    @PreAuthorize(Roles.PERSONAL)
    List<ClienteResponse> listar() {
        return listarClientes.execute();
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.PERSONAL + " or @acceso.esCliente(#id)")
    ClienteResponse obtener(@PathVariable UUID id) {
        return obtenerCliente.execute(id);
    }

    @PostMapping("/{id}/vehiculos")
    @PreAuthorize(Roles.OPERACION)
    ResponseEntity<VehiculoResponse> vehiculo(
            @PathVariable UUID id, @Valid @RequestBody CrearVehiculoRequest solicitud) {
        return ResponseEntity.status(201)
                .body(
                        crearVehiculo.execute(
                                new CrearVehiculoRequestModel(
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
    @PreAuthorize(Roles.PERSONAL + " or @acceso.esCliente(#id)")
    List<VehiculoResponse> vehiculos(@PathVariable UUID id) {
        return listarVehiculos.execute(id);
    }

    /** Nuevo: lectura de refuerzo para quotation-service (sección 5.4 de la propuesta). */
    @GetMapping("/{id}/vehiculos/{vehiculoId}")
    @PreAuthorize(Roles.PERSONAL + " or @acceso.esCliente(#id)")
    VehiculoResponse vehiculoDelCliente(@PathVariable UUID id, @PathVariable UUID vehiculoId) {
        return obtenerVehiculo.execute(id, vehiculoId);
    }
}
