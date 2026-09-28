package com.backendseguros.identity.usecases.service.auth;

import com.backendseguros.identity.entities.exception.ReglaNegocioException;
import com.backendseguros.identity.usecases.dto.Responses.PerfilResponse;
import com.backendseguros.identity.usecases.port.out.repository.UsuarioRepository;

public class ObtenerPerfilUseCase {
    private final UsuarioRepository usuarios;

    public ObtenerPerfilUseCase(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public PerfilResponse execute(String username) {
        var usuario =
                usuarios
                        .buscarPorUsername(username)
                        .orElseThrow(
                                () ->
                                        new ReglaNegocioException(
                                                "USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));
        return new PerfilResponse(
                usuario.getUsername(),
                usuario.getRol().name(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getEmail());
    }
}
