package com.andinaseguros.usecases.mapper;

import com.andinaseguros.usecases.dto.Responses.*;
import com.andinaseguros.entities.model.TablaTarifaria;

public final class TablaTarifariaResponseMapper {
    private TablaTarifariaResponseMapper() {}

    public static TablaResponse toResponse(TablaTarifaria tabla) {
        return new TablaResponse(
                tabla.getId(),
                tabla.getCodigo(),
                tabla.getVersion(),
                tabla.getTipoVehiculo(),
                tabla.getTipoUso(),
                tabla.getPrimaBase().valor(),
                tabla.getPrimaMinima().valor(),
                tabla.getVigencia().inicio(),
                tabla.getVigencia().fin(),
                tabla.getCodigoNotaTecnica(),
                tabla.getEstado());
    }

    public static TablaDetalleResponse toDetailResponse(TablaTarifaria tabla) {
        var factores =
                tabla.getFactores().stream()
                        .map(
                                factor ->
                                        new FactorResponse(
                                                factor.id(),
                                                factor.codigo(),
                                                factor.nombre(),
                                                factor.tipoVariable(),
                                                factor.valorMinimo(),
                                                factor.valorMaximo(),
                                                factor.multiplicador(),
                                                factor.orden()))
                        .toList();

        return new TablaDetalleResponse(
                tabla.getId(),
                tabla.getCodigo(),
                tabla.getVersion(),
                tabla.getTipoVehiculo(),
                tabla.getTipoUso(),
                tabla.getPrimaBase().valor(),
                tabla.getPrimaMinima().valor(),
                tabla.getVigencia().inicio(),
                tabla.getVigencia().fin(),
                tabla.getCodigoNotaTecnica(),
                tabla.getEstado(),
                factores);
    }
}
