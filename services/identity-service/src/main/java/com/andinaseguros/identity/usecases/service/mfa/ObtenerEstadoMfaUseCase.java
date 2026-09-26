package com.andinaseguros.identity.usecases.service.mfa;

import com.andinaseguros.identity.entities.exception.ReglaNegocioException;
import com.andinaseguros.identity.usecases.dto.Responses.MfaStatusResponse;
import com.andinaseguros.identity.usecases.port.out.repository.UsuarioRepository;

public class ObtenerEstadoMfaUseCase {
    private final UsuarioRepository usuarios;
    public ObtenerEstadoMfaUseCase(UsuarioRepository usuarios) { this.usuarios = usuarios; }
    public MfaStatusResponse execute(String username) {
        var usuario = usuarios.buscarPorUsername(username).orElseThrow(() -> new ReglaNegocioException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));
        return new MfaStatusResponse(usuario.isMfaHabilitado());
    }
}
