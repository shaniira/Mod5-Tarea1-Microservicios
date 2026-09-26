package com.andinaseguros.usecases.port.in;

import com.andinaseguros.usecases.model.VehicleInformation;

public interface ConsultarInformacionVehiculoUseCase {
    VehicleInformation consultarPorPlaca(String placa);
}
