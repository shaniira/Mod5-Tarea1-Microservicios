package com.andinaseguros.usecases.service.cotizacion;

import static com.andinaseguros.usecases.mapper.CotizacionResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.CotizacionResponse;
import com.andinaseguros.entities.enums.EstadoCotizacion;
import com.andinaseguros.usecases.port.out.repository.CotizacionRepository;
import com.andinaseguros.usecases.port.out.repository.PolizaRepository;
import java.util.List;

public class ListarCotizacionesPendientesEmisionUseCase {
    private final CotizacionRepository cotizaciones;
    private final PolizaRepository polizas;

    public ListarCotizacionesPendientesEmisionUseCase(
            CotizacionRepository cotizaciones, PolizaRepository polizas) {
        this.cotizaciones = cotizaciones;
        this.polizas = polizas;
    }

    public List<CotizacionResponse> execute() {
        return cotizaciones.listarPorEstado(EstadoCotizacion.ACEPTADA).stream()
                .filter(cotizacion -> polizas.buscarPorCotizacionId(cotizacion.getId()).isEmpty())
                .map(cotizacion -> toResponse(cotizacion, null))
                .toList();
    }
}
