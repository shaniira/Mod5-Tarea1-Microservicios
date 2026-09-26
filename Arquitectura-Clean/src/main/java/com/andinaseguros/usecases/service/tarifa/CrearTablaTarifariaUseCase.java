package com.andinaseguros.usecases.service.tarifa;

import static com.andinaseguros.usecases.mapper.TablaTarifariaResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.CrearFactorRequestModel;
import com.andinaseguros.usecases.dto.CrearTablaRequestModel;
import com.andinaseguros.usecases.dto.Responses.TablaResponse;
import com.andinaseguros.entities.model.FactorRiesgo;
import com.andinaseguros.entities.model.TablaTarifaria;
import com.andinaseguros.usecases.port.out.repository.TablaTarifariaRepository;
import com.andinaseguros.entities.valueobject.Dinero;
import com.andinaseguros.entities.valueobject.PeriodoVigencia;
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
