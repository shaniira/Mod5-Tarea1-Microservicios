package com.backendseguros.policy.interfaceadapters.in.rest.controller;

import com.backendseguros.policy.interfaceadapters.in.rest.security.AccesoRecursos;
import com.backendseguros.policy.interfaceadapters.in.rest.security.Roles;
import com.backendseguros.policy.usecases.dto.Responses.PolizaConRenovacionesResponse;
import com.backendseguros.policy.usecases.service.poliza.ListarMisPolizasUseCase;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Paso 6.9: la parte de "Mi cuenta" que es de policy-service. Ruta nueva: el gateway compone
 * /api/mi-cuenta con el cliente (customer-service) y estas pólizas, con respuesta parcial si uno
 * no responde (paso 3.7).
 */
@RestController
public class MiCuentaController {
    private final ListarMisPolizasUseCase misPolizas;
    private final AccesoRecursos acceso;

    public MiCuentaController(ListarMisPolizasUseCase misPolizas, AccesoRecursos acceso) {
        this.misPolizas = misPolizas;
        this.acceso = acceso;
    }

    @GetMapping("/api/mi-cuenta/polizas")
    @PreAuthorize(Roles.CLIENTE)
    public List<PolizaConRenovacionesResponse> misPolizas() {
        return misPolizas.execute(acceso.clienteDelToken().orElse(null));
    }
}
