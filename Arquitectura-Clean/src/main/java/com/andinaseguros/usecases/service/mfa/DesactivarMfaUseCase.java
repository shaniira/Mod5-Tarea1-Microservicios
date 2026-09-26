package com.andinaseguros.usecases.service.mfa;

import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.usecases.port.out.security.TotpVerifierPort;

public class DesactivarMfaUseCase {
    private final UsuarioRepository usuarios;
    private final TotpVerifierPort totp;
    public DesactivarMfaUseCase(UsuarioRepository usuarios, TotpVerifierPort totp) { this.usuarios = usuarios; this.totp = totp; }
    public void execute(String username, String codigo) {
        var usuario = usuarios.buscarPorUsername(username).orElseThrow(this::noConfigurado);
        if (!usuario.isMfaHabilitado() || usuario.getMfaSecret() == null) throw noConfigurado();
        if (!totp.verificar(usuario.getMfaSecret(), codigo)) throw new ReglaNegocioException("MFA_CODIGO_INVALIDO", "El código MFA no es válido");
        usuarios.guardar(usuario.sinMfa());
    }
    private ReglaNegocioException noConfigurado() { return new ReglaNegocioException("MFA_NO_CONFIGURADO", "MFA no está configurado"); }
}
