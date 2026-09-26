package com.andinaseguros.usecases.port.in;

import com.andinaseguros.usecases.dto.CrearCotizacionRequestModel;
import com.andinaseguros.usecases.dto.Responses.CotizacionResponse;

public interface CrearCotizacionInputPort {
    CotizacionResponse execute(CrearCotizacionRequestModel solicitud);
}
