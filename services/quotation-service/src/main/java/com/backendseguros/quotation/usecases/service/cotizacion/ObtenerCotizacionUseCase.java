package com.backendseguros.quotation.usecases.service.cotizacion;

import static com.backendseguros.quotation.usecases.mapper.CotizacionResponseMapper.toResponse;

import com.backendseguros.quotation.usecases.dto.Responses.CotizacionResponse;
import com.backendseguros.quotation.entities.exception.RecursoNoEncontradoException;
import com.backendseguros.quotation.entities.model.Cotizacion;
import com.backendseguros.quotation.usecases.port.out.repository.CotizacionRepository;
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
