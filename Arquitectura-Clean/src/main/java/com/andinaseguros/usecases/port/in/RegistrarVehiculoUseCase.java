package com.andinaseguros.usecases.port.in;

import com.andinaseguros.usecases.dto.CrearVehiculoRequestModel;
import com.andinaseguros.usecases.dto.Responses.VehiculoResponse;

public interface RegistrarVehiculoUseCase {
    VehiculoResponse execute(CrearVehiculoRequestModel solicitud);
}
