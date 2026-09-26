package com.andinaseguros.usecases.service.tarifa;

import static com.andinaseguros.usecases.mapper.TablaTarifariaResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.TablaResponse;
import com.andinaseguros.usecases.port.out.repository.TablaTarifariaRepository;
import java.util.List;

public class ListarTablasTarifariasUseCase {

    private final TablaTarifariaRepository tablaTarifariaRepository;

    public ListarTablasTarifariasUseCase(TablaTarifariaRepository tablaTarifariaRepository) {
        this.tablaTarifariaRepository = tablaTarifariaRepository;
    }

    public List<TablaResponse> execute() {
        return tablaTarifariaRepository.listar().stream().map(tabla -> toResponse(tabla)).toList();
    }
}
