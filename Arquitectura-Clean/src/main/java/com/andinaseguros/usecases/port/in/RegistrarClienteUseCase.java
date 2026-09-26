package com.andinaseguros.usecases.port.in;

import com.andinaseguros.usecases.dto.CrearClienteRequestModel;
import com.andinaseguros.usecases.dto.Responses.ClienteResponse;

public interface RegistrarClienteUseCase {
    ClienteResponse execute(CrearClienteRequestModel solicitud);
}
