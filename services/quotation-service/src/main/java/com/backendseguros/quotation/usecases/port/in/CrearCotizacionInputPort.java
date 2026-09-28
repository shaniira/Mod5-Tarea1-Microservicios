package com.backendseguros.quotation.usecases.port.in;

import com.backendseguros.quotation.usecases.dto.CrearCotizacionRequestModel;
import com.backendseguros.quotation.usecases.dto.Responses.CotizacionResponse;

public interface CrearCotizacionInputPort {
    CotizacionResponse execute(CrearCotizacionRequestModel solicitud);
}
