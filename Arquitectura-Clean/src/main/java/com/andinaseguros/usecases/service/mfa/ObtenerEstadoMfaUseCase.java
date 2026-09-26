package com.andinaseguros.usecases.service.mfa;

import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.usecases.dto.Responses.MfaStatusResponse;
import com.andinaseguros.usecases.port.out.repository.UsuarioRepository;

public class ObtenerEstadoMfaUseCase {
    private final UsuarioRepository usuarios;
    public ObtenerEstadoMfaUseCase(UsuarioRepository usuarios) { this.usuarios = usuarios; }
    public MfaStatusResponse execute(String username) {
        var usuario = usuarios.buscarPorUsername(username).orElseThrow(() -> new ReglaNegocioException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));
        return new MfaStatusResponse(usuario.isMfaHabilitado());
    }
}
