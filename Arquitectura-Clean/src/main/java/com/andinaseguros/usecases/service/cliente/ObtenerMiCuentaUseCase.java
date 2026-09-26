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

    public MiCuentaResponse execute(String username) {
        var correo =
                usuarios.buscarPorUsername(username)
                        .map(usuario -> usuario.getEmail() != null ? usuario.getEmail() : usuario.getUsername())
                        .orElse(username);

        var cliente = clientes.buscarPorCorreo(correo);
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
