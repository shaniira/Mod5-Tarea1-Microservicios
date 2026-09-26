package com.andinaseguros.usecases.service.cliente;

import com.andinaseguros.entities.model.Cliente;
import com.andinaseguros.usecases.dto.Responses.MiCuentaResponse;
import com.andinaseguros.usecases.dto.Responses.PolizaConRenovacionesResponse;
import com.andinaseguros.usecases.mapper.ClienteResponseMapper;
import com.andinaseguros.usecases.mapper.PolizaResponseMapper;
import com.andinaseguros.usecases.mapper.RenovacionResponseMapper;
import com.andinaseguros.usecases.port.out.repository.ClienteRepository;
import com.andinaseguros.usecases.port.out.repository.PolizaRepository;
import com.andinaseguros.usecases.port.out.repository.RenovacionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fase 2: los usuarios viven en identity-service. El token trae el customerId del cliente, así que
 * aquí no se consulta ninguna colección de usuarios.
 */
public class ObtenerMiCuentaUseCase {
    private final ClienteRepository clientes;
    private final PolizaRepository polizas;
    private final RenovacionRepository renovaciones;

    public ObtenerMiCuentaUseCase(
            ClienteRepository clientes,
            PolizaRepository polizas,
            RenovacionRepository renovaciones) {
        this.clientes = clientes;
        this.polizas = polizas;
        this.renovaciones = renovaciones;
    }

    /**
     * Sin customerId (un usuario CLIENTE cuyo correo aún no está registrado como cliente) se prueba
     * con el username, que para los clientes es su correo.
     */
    public MiCuentaResponse execute(String username, UUID customerId) {
        Optional<Cliente> cliente =
                customerId != null ? clientes.buscarPorId(customerId) : clientes.buscarPorCorreo(username);
        if (cliente.isEmpty()) {
            return new MiCuentaResponse(null, List.of());
        }

        var misPolizas =
                polizas.listarPorCliente(cliente.get().getId()).stream()
                        .map(
                                poliza ->
                                        new PolizaConRenovacionesResponse(
                                                PolizaResponseMapper.toResponse(poliza),
                                                renovaciones.listarPorPoliza(poliza.getId()).stream()
                                                        .map(RenovacionResponseMapper::toResponse)
                                                        .toList()))
                        .toList();

        return new MiCuentaResponse(ClienteResponseMapper.toResponse(cliente.get()), misPolizas);
    }
}
