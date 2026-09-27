package com.andinaseguros.customer.interfaceadapters.in.rest.controller;

import com.andinaseguros.customer.interfaceadapters.in.rest.response.VehicleInformationResponse;
import com.andinaseguros.customer.interfaceadapters.in.rest.security.Roles;
import com.andinaseguros.customer.usecases.port.in.ConsultarInformacionVehiculoUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
