package com.andinaseguros.identity.usecases.service.auth;

import com.andinaseguros.identity.entities.exception.ReglaNegocioException;
import com.andinaseguros.identity.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.identity.usecases.port.out.security.RevocacionPort;
import java.time.Instant;

/**
 * Cierre de sesiones. Como los demás servicios ya no consultan la colección usuarios, desactivar
 * un usuario no basta: además se revocan sus tokens para que dejen de valer al instante (y no al
 * vencer, hasta 8 h después). El logout revoca el token actual (riesgo S8).
 */
public class GestionarSesionesUseCase {
    private final UsuarioRepository usuarios;
    private final RevocacionPort revocaciones;

    public GestionarSesionesUseCase(UsuarioRepository usuarios, RevocacionPort revocaciones) {
        this.usuarios = usuarios;
        this.revocaciones = revocaciones;
    }

    public void cambiarEstado(String username, boolean activo) {
        var usuario =
                usuarios.buscarPorUsername(username)
                        .orElseThrow(
                                () -> new ReglaNegocioException("RECURSO_NO_ENCONTRADO", "Usuario no encontrado"));
        usuarios.guardar(usuario.conActivo(activo));
        if (activo) {
            revocaciones.restaurarUsuario(username);
        } else {
            revocaciones.revocarUsuario(username);
        }
    }

    public void cerrarSesion(String jti, Instant expiraEn) {
        if (jti != null && expiraEn != null) {
            revocaciones.revocarToken(jti, expiraEn);
        }
    }
}
