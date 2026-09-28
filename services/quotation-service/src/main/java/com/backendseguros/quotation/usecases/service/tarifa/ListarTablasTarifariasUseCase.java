package com.backendseguros.quotation.usecases.service.tarifa;

import static com.backendseguros.quotation.usecases.mapper.TablaTarifariaResponseMapper.toResponse;

import com.backendseguros.quotation.usecases.dto.Responses.TablaResponse;
import com.backendseguros.quotation.usecases.port.out.repository.TablaTarifariaRepository;
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
