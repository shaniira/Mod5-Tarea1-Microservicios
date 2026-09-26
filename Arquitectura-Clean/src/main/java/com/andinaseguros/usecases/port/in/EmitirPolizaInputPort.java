package com.andinaseguros.usecases.port.in;

import com.andinaseguros.usecases.dto.EmitirPolizaRequestModel;
import com.andinaseguros.usecases.dto.Responses.PolizaResponse;

public interface EmitirPolizaInputPort {
    PolizaResponse execute(EmitirPolizaRequestModel solicitud);
}
