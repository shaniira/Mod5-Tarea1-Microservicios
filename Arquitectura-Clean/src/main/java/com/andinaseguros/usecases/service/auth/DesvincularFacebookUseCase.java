package com.andinaseguros.usecases.service.auth;

import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.usecases.port.out.repository.UsuarioRepository;

public class DesvincularFacebookUseCase {
    private final UsuarioRepository usuarios;

    public DesvincularFacebookUseCase(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public void execute(String username) {
        var usuario = usuarios.buscarPorUsername(username)
                .orElseThrow(() -> new ReglaNegocioException("RECURSO_NO_ENCONTRADO", "Usuario no encontrado"));
        if (!"FACEBOOK".equals(usuario.getProvider())) {
            throw new ReglaNegocioException("FACEBOOK_NO_VINCULADO", "El usuario no tiene una cuenta Facebook vinculada");
        }
        usuarios.guardar(usuario.sinAutorizacionFacebook());
    }
}