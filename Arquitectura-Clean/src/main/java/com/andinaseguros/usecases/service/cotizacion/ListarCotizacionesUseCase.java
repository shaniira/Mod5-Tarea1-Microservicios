package com.andinaseguros.usecases.service.cotizacion;

import static com.andinaseguros.usecases.mapper.CotizacionResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.CotizacionResponse;
import com.andinaseguros.entities.enums.EstadoCotizacion;
import com.andinaseguros.usecases.port.out.repository.CotizacionRepository;
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
