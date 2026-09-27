package com.andinaseguros.quotation.usecases.service.cotizacion;

import static com.andinaseguros.quotation.usecases.mapper.CotizacionResponseMapper.toResponse;

import com.andinaseguros.quotation.usecases.dto.Responses.CotizacionResponse;
import com.andinaseguros.quotation.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.quotation.entities.model.Cotizacion;
import com.andinaseguros.quotation.usecases.port.out.repository.CotizacionRepository;
import java.util.UUID;

public class ObtenerCotizacionUseCase {

    private final CotizacionRepository cotizacionRepository;

    public ObtenerCotizacionUseCase(CotizacionRepository cotizacionRepository) {
        this.cotizacionRepository = cotizacionRepository;
    }

    public CotizacionResponse execute(UUID cotizacionId) {
        Cotizacion cotizacion =
                cotizacionRepository
                        .buscarPorId(cotizacionId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Cotización"));

        return toResponse(cotizacion, null);
    }
}
