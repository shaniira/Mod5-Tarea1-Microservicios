package com.andinaseguros.usecases.service.cotizacion;

import static com.andinaseguros.usecases.mapper.CotizacionResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.CotizacionResponse;
import com.andinaseguros.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.entities.model.Cotizacion;
import com.andinaseguros.usecases.port.out.repository.CotizacionRepository;
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
