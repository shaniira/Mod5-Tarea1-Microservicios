package com.andinaseguros.quotation.usecases.service.cotizacion;

import static com.andinaseguros.quotation.usecases.mapper.CotizacionResponseMapper.toResponse;

import com.andinaseguros.quotation.usecases.dto.Responses.CotizacionResponse;
import com.andinaseguros.quotation.entities.enums.EstadoCotizacion;
import com.andinaseguros.quotation.usecases.port.out.repository.CotizacionRepository;
import java.util.List;

public class ListarCotizacionesUseCase {
    private final CotizacionRepository repository;

    public ListarCotizacionesUseCase(CotizacionRepository repository) {
        this.repository = repository;
    }

    public List<CotizacionResponse> execute(EstadoCotizacion estado) {
        var cotizaciones =
                estado == null ? repository.listar() : repository.listarPorEstado(estado);
        return cotizaciones.stream().map(cotizacion -> toResponse(cotizacion, null)).toList();
    }
}
