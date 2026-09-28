package com.backendseguros.customer.usecases.dto;

import com.backendseguros.customer.entities.enums.TipoUso;
import com.backendseguros.customer.entities.enums.TipoVehiculo;
import java.util.UUID;

public record CrearVehiculoRequestModel(
        UUID clienteId,
        String placa,
        String marca,
        String modelo,
        int anioFabricacion,
        TipoVehiculo tipo,
        TipoUso uso,
        String zonaCirculacion) {}
