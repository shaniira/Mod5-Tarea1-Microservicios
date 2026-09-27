package com.andinaseguros.customer.usecases.port.in;

import com.andinaseguros.customer.usecases.dto.CrearClienteRequestModel;
import com.andinaseguros.customer.usecases.dto.Responses.ClienteResponse;

public interface RegistrarClienteUseCase {
    ClienteResponse execute(CrearClienteRequestModel solicitud);
}
