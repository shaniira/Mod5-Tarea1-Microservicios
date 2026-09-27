package com.andinaseguros.customer.usecases.dto;

import com.andinaseguros.customer.entities.enums.TipoUso;
import com.andinaseguros.customer.entities.enums.TipoVehiculo;
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
