package com.andinaseguros.customer.usecases.port.in;

import com.andinaseguros.customer.usecases.model.VehicleInformation;

public interface ConsultarInformacionVehiculoUseCase {
    VehicleInformation consultarPorPlaca(String placa);
}
