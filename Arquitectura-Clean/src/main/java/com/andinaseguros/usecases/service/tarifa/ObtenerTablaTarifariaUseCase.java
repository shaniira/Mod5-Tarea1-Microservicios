package com.andinaseguros.usecases.service.tarifa;

import static com.andinaseguros.usecases.mapper.TablaTarifariaResponseMapper.toDetailResponse;

import com.andinaseguros.usecases.dto.Responses.TablaDetalleResponse;
import com.andinaseguros.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.entities.model.TablaTarifaria;
import com.andinaseguros.usecases.port.out.repository.TablaTarifariaRepository;
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
