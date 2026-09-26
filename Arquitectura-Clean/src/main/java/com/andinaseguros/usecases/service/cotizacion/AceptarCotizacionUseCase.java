package com.andinaseguros.usecases.service.cotizacion;

import static com.andinaseguros.usecases.mapper.CotizacionResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.CotizacionResponse;
import com.andinaseguros.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.entities.model.Cotizacion;
import com.andinaseguros.usecases.port.out.repository.CotizacionRepository;
import java.util.UUID;

public class AceptarCotizacionUseCase {

    private final CotizacionRepository cotizacionRepository;

    public AceptarCotizacionUseCase(CotizacionRepository cotizacionRepository) {
        this.cotizacionRepository = cotizacionRepository;
    }

    public CotizacionResponse execute(UUID cotizacionId) {
        Cotizacion cotizacion =
                cotizacionRepository
                        .buscarPorId(cotizacionId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Cotización"));

        cotizacion.aceptar();

        Cotizacion cotizacionGuardada = cotizacionRepository.guardar(cotizacion);

        return toResponse(cotizacionGuardada, null);
    }
}
