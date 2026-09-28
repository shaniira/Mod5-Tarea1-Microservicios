package com.backendseguros.quotation.usecases.service.cotizacion;

import static com.backendseguros.quotation.usecases.mapper.CotizacionResponseMapper.toResponse;

import com.backendseguros.quotation.entities.enums.EstadoCotizacion;
import com.backendseguros.quotation.usecases.dto.Responses.CotizacionResponse;
import com.backendseguros.quotation.usecases.port.out.repository.CotizacionRepository;
import java.util.List;

/**
 * Cotizaciones aceptadas que aún no tienen póliza. El monolito lo resolvía consultando la base de
 * pólizas; aquí basta el estado, porque policy.issued.v1 marca la cotización como EMITIDA
 * (MarcarCotizacionEmitidaUseCase).
 */
public class ListarCotizacionesPendientesEmisionUseCase {
    private final CotizacionRepository cotizaciones;

    public ListarCotizacionesPendientesEmisionUseCase(CotizacionRepository cotizaciones) {
        this.cotizaciones = cotizaciones;
    }

    public List<CotizacionResponse> execute() {
        return cotizaciones.listarPorEstado(EstadoCotizacion.ACEPTADA).stream()
                .map(cotizacion -> toResponse(cotizacion, null))
                .toList();
    }
}
