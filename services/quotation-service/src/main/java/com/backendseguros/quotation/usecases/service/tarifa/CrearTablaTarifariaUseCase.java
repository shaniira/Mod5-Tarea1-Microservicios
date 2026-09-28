package com.backendseguros.quotation.usecases.service.tarifa;

import static com.backendseguros.quotation.usecases.mapper.TablaTarifariaResponseMapper.toResponse;

import com.backendseguros.quotation.usecases.dto.CrearFactorRequestModel;
import com.backendseguros.quotation.usecases.dto.CrearTablaRequestModel;
import com.backendseguros.quotation.usecases.dto.Responses.TablaResponse;
import com.backendseguros.quotation.entities.model.FactorRiesgo;
import com.backendseguros.quotation.entities.model.TablaTarifaria;
import com.backendseguros.quotation.usecases.port.out.repository.TablaTarifariaRepository;
import com.backendseguros.quotation.entities.valueobject.Dinero;
import com.backendseguros.quotation.entities.valueobject.PeriodoVigencia;
import java.util.List;
import java.util.UUID;

public class CrearTablaTarifariaUseCase {

    private final TablaTarifariaRepository tablaTarifariaRepository;

    public CrearTablaTarifariaUseCase(TablaTarifariaRepository tablaTarifariaRepository) {
        this.tablaTarifariaRepository = tablaTarifariaRepository;
    }

    public TablaResponse execute(CrearTablaRequestModel solicitud) {
        List<FactorRiesgo> factores = crearFactores(solicitud.factores());

        TablaTarifaria tablaTarifaria =
                new TablaTarifaria(
                        UUID.randomUUID(),
                        solicitud.codigo(),
                        solicitud.version(),
                        solicitud.tipoVehiculo(),
                        solicitud.tipoUso(),
                        Dinero.soles(solicitud.primaBase()),
                        Dinero.soles(solicitud.primaMinima()),
                        new PeriodoVigencia(solicitud.inicioVigencia(), solicitud.finVigencia()),
                        solicitud.codigoNotaTecnica(),
                        solicitud.estado(),
                        factores);

        TablaTarifaria tablaGuardada = tablaTarifariaRepository.guardar(tablaTarifaria);

        return toResponse(tablaGuardada);
    }

    private List<FactorRiesgo> crearFactores(List<CrearFactorRequestModel> solicitudes) {
        List<CrearFactorRequestModel> factores = solicitudes == null ? List.of() : solicitudes;

        return factores.stream().map(this::crearFactor).toList();
    }

    private FactorRiesgo crearFactor(CrearFactorRequestModel solicitud) {
        return new FactorRiesgo(
                UUID.randomUUID(),
                solicitud.codigo(),
                solicitud.nombre(),
                solicitud.tipoVariable(),
                solicitud.valorMinimo(),
                solicitud.valorMaximo(),
                solicitud.multiplicador(),
                solicitud.orden());
    }
}
