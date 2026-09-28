package com.backendseguros.customer.usecases.port.in;

import com.backendseguros.customer.usecases.model.VehicleInformation;

public interface ConsultarInformacionVehiculoUseCase {
    VehicleInformation consultarPorPlaca(String placa);
}
