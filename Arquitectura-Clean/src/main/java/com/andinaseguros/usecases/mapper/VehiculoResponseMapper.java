package com.andinaseguros.usecases.mapper;

import com.andinaseguros.usecases.dto.Responses.VehiculoResponse;
import com.andinaseguros.entities.model.Vehiculo;

public final class VehiculoResponseMapper {
    private VehiculoResponseMapper() {}

    public static VehiculoResponse toResponse(Vehiculo vehiculo) {
        return new VehiculoResponse(
                vehiculo.getId(),
                vehiculo.getClienteId(),
                vehiculo.getPlaca().valor(),
                vehiculo.getMarca(),
                vehiculo.getModelo(),
                vehiculo.getAnioFabricacion(),
                vehiculo.getTipo(),
                vehiculo.getUso(),
                vehiculo.getZonaCirculacion());
    }
}
