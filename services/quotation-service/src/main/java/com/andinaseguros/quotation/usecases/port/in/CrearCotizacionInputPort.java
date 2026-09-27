package com.andinaseguros.quotation.usecases.port.in;

import com.andinaseguros.quotation.usecases.dto.CrearCotizacionRequestModel;
import com.andinaseguros.quotation.usecases.dto.Responses.CotizacionResponse;

public interface CrearCotizacionInputPort {
    CotizacionResponse execute(CrearCotizacionRequestModel solicitud);
}
