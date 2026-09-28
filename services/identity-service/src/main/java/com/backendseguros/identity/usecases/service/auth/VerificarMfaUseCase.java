package com.backendseguros.identity.usecases.service.auth;

import com.backendseguros.identity.entities.exception.ReglaNegocioException;
import com.backendseguros.identity.usecases.dto.MfaVerifyRequestModel;
import com.backendseguros.identity.usecases.dto.Responses.TokenResponse;
import com.backendseguros.identity.usecases.port.out.repository.UsuarioRepository;
import com.backendseguros.identity.usecases.port.out.security.*;

public class VerificarMfaUseCase {
    private final UsuarioRepository usuarios;
    private final MfaChallengePort challenges;
    private final TotpVerifierPort totp;
    private final EmisorDeTokens emisor;
    public VerificarMfaUseCase(UsuarioRepository usuarios, MfaChallengePort challenges, TotpVerifierPort totp, EmisorDeTokens emisor) {
        this.usuarios = usuarios; this.challenges = challenges; this.totp = totp; this.emisor = emisor;
    }
    public TokenResponse execute(MfaVerifyRequestModel solicitud) {
        AuthenticatedUser identity = challenges.consumir(solicitud.challengeToken());
        var usuario = usuarios.buscarPorUsername(identity.username()).orElseThrow(this::codigoInvalido);
        if (!usuario.isActivo() || !usuario.isMfaHabilitado() || !totp.verificar(usuario.getMfaSecret(), solicitud.codigo())) throw codigoInvalido();
        return emisor.emitir(identity);
    }
    private ReglaNegocioException codigoInvalido() { return new ReglaNegocioException("MFA_CODIGO_INVALIDO", "El código MFA no es válido"); }
}
