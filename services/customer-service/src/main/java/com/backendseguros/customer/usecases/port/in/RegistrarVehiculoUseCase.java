package com.backendseguros.customer.usecases.port.in;

import com.backendseguros.customer.usecases.dto.CrearVehiculoRequestModel;
import com.backendseguros.customer.usecases.dto.Responses.VehiculoResponse;

public interface RegistrarVehiculoUseCase {
    VehiculoResponse execute(CrearVehiculoRequestModel solicitud);
}
