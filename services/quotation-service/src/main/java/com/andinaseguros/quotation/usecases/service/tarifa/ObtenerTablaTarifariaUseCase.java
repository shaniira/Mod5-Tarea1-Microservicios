package com.andinaseguros.quotation.usecases.service.tarifa;

import static com.andinaseguros.quotation.usecases.mapper.TablaTarifariaResponseMapper.toDetailResponse;

import com.andinaseguros.quotation.usecases.dto.Responses.TablaDetalleResponse;
import com.andinaseguros.quotation.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.quotation.entities.model.TablaTarifaria;
import com.andinaseguros.quotation.usecases.port.out.repository.TablaTarifariaRepository;
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
