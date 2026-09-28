package com.backendseguros.quotation.interfaceadapters.in.rest.mapper;

import com.backendseguros.quotation.interfaceadapters.in.rest.request.CrearCotizacionRequest;
import com.backendseguros.quotation.interfaceadapters.in.rest.request.CrearFactorRequest;
import com.backendseguros.quotation.interfaceadapters.in.rest.request.CrearTablaRequest;
import com.backendseguros.quotation.usecases.dto.CrearCotizacionRequestModel;
import com.backendseguros.quotation.usecases.dto.CrearFactorRequestModel;
import com.backendseguros.quotation.usecases.dto.CrearTablaRequestModel;
import java.util.List;

public final class RestRequestMapper {
    private RestRequestMapper() {}

    public static CrearCotizacionRequestModel toCore(CrearCotizacionRequest value) {
        return new CrearCotizacionRequestModel(
                value.clienteId(),
                value.vehiculoId(),
                value.siniestrosResponsables(),
                value.porcentajeGastos(),
                value.porcentajeRecargo(),
                value.porcentajeDescuento());
    }

    public static CrearTablaRequestModel toCore(CrearTablaRequest value) {
        List<CrearFactorRequestModel> factores =
                value.factores() == null
                        ? List.of()
                        : value.factores().stream().map(RestRequestMapper::toCore).toList();
        return new CrearTablaRequestModel(
                value.codigo(),
                value.version(),
                value.tipoVehiculo(),
                value.tipoUso(),
                value.primaBase(),
                value.primaMinima(),
                value.inicioVigencia(),
                value.finVigencia(),
                value.codigoNotaTecnica(),
                value.estado(),
                factores);
    }

    private static CrearFactorRequestModel toCore(CrearFactorRequest value) {
        return new CrearFactorRequestModel(
                value.codigo(),
                value.nombre(),
                value.tipoVariable(),
                value.valorMinimo(),
                value.valorMaximo(),
                value.multiplicador(),
                value.orden());
    }
}
