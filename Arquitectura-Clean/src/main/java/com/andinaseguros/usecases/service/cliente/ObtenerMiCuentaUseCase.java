package com.andinaseguros.usecases.service.cliente;

import com.andinaseguros.usecases.dto.Responses.MiCuentaResponse;
import com.andinaseguros.usecases.dto.Responses.PolizaConRenovacionesResponse;
import com.andinaseguros.usecases.mapper.ClienteResponseMapper;
import com.andinaseguros.usecases.mapper.PolizaResponseMapper;
import com.andinaseguros.usecases.mapper.RenovacionResponseMapper;
import com.andinaseguros.usecases.port.out.repository.ClienteRepository;
import com.andinaseguros.usecases.port.out.repository.PolizaRepository;
import com.andinaseguros.usecases.port.out.repository.RenovacionRepository;
import com.andinaseguros.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.entities.model.Cliente;
import java.util.Optional;
import java.util.UUID;

public class ObtenerMiCuentaUseCase {
    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final PolizaRepository polizas;
    private final RenovacionRepository renovaciones;

    public ObtenerMiCuentaUseCase(
            UsuarioRepository usuarios,
            ClienteRepository clientes,
            PolizaRepository polizas,
            RenovacionRepository renovaciones) {
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.polizas = polizas;
        this.renovaciones = renovaciones;
    }

    /**
     * Fase 2: identity-service pone en el token el customerId del cliente; con él no hace falta
     * consultar usuarios. Sin customerId (tokens antiguos de la ventana de transición) se usa la
     * búsqueda por correo de siempre.
     */
    public MiCuentaResponse execute(String username, UUID customerId) {
        if (customerId == null) {
            return execute(username);
        }
        return responder(clientes.buscarPorId(customerId));
    }

    public MiCuentaResponse execute(String username) {
        var correo =
                usuarios.buscarPorUsername(username)
                        .map(usuario -> usuario.getEmail() != null ? usuario.getEmail() : usuario.getUsername())
                        .orElse(username);

        return responder(clientes.buscarPorCorreo(correo));
    }

    private MiCuentaResponse responder(Optional<Cliente> cliente) {
        if (cliente.isEmpty()) {
            return new MiCuentaResponse(null, java.util.List.of());
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
