package com.andinaseguros.interfaceadapters.in.rest.controller;

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
    public ResponseEntity<VehicleInformationResponse> consultar(@RequestParam String placa) {
        return ResponseEntity.ok(VehicleInformationResponse.from(useCase.consultarPorPlaca(placa)));
    }
}
