package com.backendseguros.customer.usecases.port.in;

import com.backendseguros.customer.usecases.dto.CrearClienteRequestModel;
import com.backendseguros.customer.usecases.dto.Responses.ClienteResponse;

public interface RegistrarClienteUseCase {
    ClienteResponse execute(CrearClienteRequestModel solicitud);
}
