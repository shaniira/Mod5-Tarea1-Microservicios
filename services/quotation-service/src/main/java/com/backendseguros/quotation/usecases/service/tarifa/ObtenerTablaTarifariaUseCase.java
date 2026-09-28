package com.backendseguros.quotation.usecases.service.tarifa;

import static com.backendseguros.quotation.usecases.mapper.TablaTarifariaResponseMapper.toDetailResponse;

import com.backendseguros.quotation.usecases.dto.Responses.TablaDetalleResponse;
import com.backendseguros.quotation.entities.exception.RecursoNoEncontradoException;
import com.backendseguros.quotation.entities.model.TablaTarifaria;
import com.backendseguros.quotation.usecases.port.out.repository.TablaTarifariaRepository;
import java.util.UUID;

public class ObtenerTablaTarifariaUseCase {

    private final TablaTarifariaRepository tablaTarifariaRepository;

    public ObtenerTablaTarifariaUseCase(TablaTarifariaRepository tablaTarifariaRepository) {
        this.tablaTarifariaRepository = tablaTarifariaRepository;
    }

    public TablaDetalleResponse execute(UUID tablaTarifariaId) {
        TablaTarifaria tablaTarifaria =
                tablaTarifariaRepository
                        .buscarPorId(tablaTarifariaId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Tabla tarifaria"));

        return toDetailResponse(tablaTarifaria);
    }
}
