package com.backendseguros.customer.usecases.mapper;

import com.backendseguros.customer.usecases.dto.Responses.VehiculoResponse;
import com.backendseguros.customer.entities.model.Vehiculo;

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
