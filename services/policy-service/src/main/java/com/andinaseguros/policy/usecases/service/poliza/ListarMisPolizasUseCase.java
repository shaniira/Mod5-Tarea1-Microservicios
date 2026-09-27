package com.andinaseguros.policy.usecases.service.poliza;

import com.andinaseguros.policy.usecases.dto.Responses.PolizaConRenovacionesResponse;
import com.andinaseguros.policy.usecases.mapper.PolizaResponseMapper;
import com.andinaseguros.policy.usecases.mapper.RenovacionResponseMapper;
import com.andinaseguros.policy.usecases.port.out.repository.PolizaRepository;
import com.andinaseguros.policy.usecases.port.out.repository.RenovacionRepository;
import java.util.List;
import java.util.UUID;

/**
 * La parte de "Mi cuenta" que es de policy-service (paso 6.9): las pólizas del cliente del token con
 * sus renovaciones, en el mismo formato que la respuesta del monolito. El gateway la compone con el
 * cliente de customer-service.
 */
public class ListarMisPolizasUseCase {
    private final PolizaRepository polizas;
    private final RenovacionRepository renovaciones;

    public ListarMisPolizasUseCase(PolizaRepository polizas, RenovacionRepository renovaciones) {
        this.polizas = polizas;
        this.renovaciones = renovaciones;
    }

    public List<PolizaConRenovacionesResponse> execute(UUID clienteId) {
        if (clienteId == null) {
            return List.of();
        }
        return polizas.listarPorCliente(clienteId).stream()
                .map(
                        poliza ->
                                new PolizaConRenovacionesResponse(
                                        PolizaResponseMapper.toResponse(poliza),
                                        renovaciones.listarPorPoliza(poliza.getId()).stream()
                                                .map(RenovacionResponseMapper::toResponse)
                                                .toList()))
                .toList();
    }
}
