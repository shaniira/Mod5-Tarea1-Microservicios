package com.andinaseguros.customer.usecases.port.in;

import com.andinaseguros.customer.usecases.dto.CrearVehiculoRequestModel;
import com.andinaseguros.customer.usecases.dto.Responses.VehiculoResponse;

public interface RegistrarVehiculoUseCase {
    VehiculoResponse execute(CrearVehiculoRequestModel solicitud);
}
