package com.andinaseguros.interfaceadapters.in.rest.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.andinaseguros.interfaceadapters.in.rest.security.Roles;
import com.andinaseguros.usecases.port.in.ConsultarInformacionVehiculoUseCase;
import com.andinaseguros.interfaceadapters.in.rest.response.VehicleInformationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehiculos")
public class VehicleInformationController {
    private final ConsultarInformacionVehiculoUseCase useCase;

    public VehicleInformationController(ConsultarInformacionVehiculoUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/informacion-externa")
    @PreAuthorize(Roles.OPERACION)
    public ResponseEntity<VehicleInformationResponse> consultar(@RequestParam String placa) {
        return ResponseEntity.ok(VehicleInformationResponse.from(useCase.consultarPorPlaca(placa)));
    }
}
