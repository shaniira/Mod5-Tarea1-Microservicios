package com.andinaseguros.claims.interfaceadapters.in.rest.controller;

import com.andinaseguros.claims.interfaceadapters.in.rest.request.ActualizarEstadoSiniestroRequest;
import com.andinaseguros.claims.interfaceadapters.in.rest.request.RegistrarSiniestroRequest;
import com.andinaseguros.claims.interfaceadapters.in.rest.security.Roles;
import com.andinaseguros.claims.usecases.dto.RegistrarSiniestroRequestModel;
import com.andinaseguros.claims.usecases.dto.Responses.SiniestroResponse;
import com.andinaseguros.claims.usecases.service.siniestro.ActualizarEstadoSiniestroUseCase;
import com.andinaseguros.claims.usecases.service.siniestro.ListarSiniestrosUseCase;
import com.andinaseguros.claims.usecases.service.siniestro.RegistrarSiniestroUseCase;
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

/**
 * Mismas rutas, reglas de acceso y respuestas que el monolito. La ruta cuelga de /api/polizas: en el
 * gateway debe declararse antes que /api/polizas/** (paso 4.7).
 */
@RestController
@RequestMapping("/api/polizas/{polizaId}/siniestros")
public class SiniestroController {
    private final RegistrarSiniestroUseCase registrarSiniestro;
    private final ActualizarEstadoSiniestroUseCase actualizarEstado;
    private final ListarSiniestrosUseCase listarSiniestros;

    public SiniestroController(
            RegistrarSiniestroUseCase registrarSiniestro,
            ActualizarEstadoSiniestroUseCase actualizarEstado,
            ListarSiniestrosUseCase listarSiniestros) {
        this.registrarSiniestro = registrarSiniestro;
        this.actualizarEstado = actualizarEstado;
        this.listarSiniestros = listarSiniestros;
    }

    @PostMapping
    @PreAuthorize(Roles.OPERACION)
    public ResponseEntity<SiniestroResponse> crear(
            @PathVariable UUID polizaId, @Valid @RequestBody RegistrarSiniestroRequest solicitud) {
        return ResponseEntity.status(201)
                .body(
                        registrarSiniestro.execute(
                                new RegistrarSiniestroRequestModel(
                                        polizaId,
                                        solicitud.fecha(),
                                        solicitud.tipo(),
                                        solicitud.montoEstimado(),
                                        solicitud.responsabilidadAsegurado(),
                                        solicitud.gravedad(),
                                        solicitud.estado())));
    }

    @GetMapping
    @PreAuthorize(Roles.PERSONAL + " or @acceso.esDuenoDePoliza(#polizaId)")
    public List<SiniestroResponse> listar(@PathVariable UUID polizaId) {
        return listarSiniestros.execute(polizaId);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize(Roles.OPERACION)
    public SiniestroResponse actualizar(
            @PathVariable UUID polizaId,
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarEstadoSiniestroRequest solicitud) {
        return actualizarEstado.execute(polizaId, id, solicitud.estado());
    }
}
