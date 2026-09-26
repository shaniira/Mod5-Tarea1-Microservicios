package com.andinaseguros.usecases.service.auth;

import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.usecases.dto.MfaVerifyRequestModel;
import com.andinaseguros.usecases.dto.Responses.TokenResponse;
import com.andinaseguros.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.usecases.port.out.security.*;

public class VerificarMfaUseCase {
    private final UsuarioRepository usuarios;
    private final MfaChallengePort challenges;
    private final TotpVerifierPort totp;
    private final TokenGeneratorPort tokens;
    public VerificarMfaUseCase(UsuarioRepository usuarios, MfaChallengePort challenges, TotpVerifierPort totp, TokenGeneratorPort tokens) {
        this.usuarios = usuarios; this.challenges = challenges; this.totp = totp; this.tokens = tokens;
    }
    public TokenResponse execute(MfaVerifyRequestModel solicitud) {
        AuthenticatedUser identity = challenges.consumir(solicitud.challengeToken());
        var usuario = usuarios.buscarPorUsername(identity.username()).orElseThrow(this::codigoInvalido);
        if (!usuario.isActivo() || !usuario.isMfaHabilitado() || !totp.verificar(usuario.getMfaSecret(), solicitud.codigo())) throw codigoInvalido();
        return new TokenResponse(tokens.generar(identity), "Bearer", tokens.expirationSeconds());
    }
    private ReglaNegocioException codigoInvalido() { return new ReglaNegocioException("MFA_CODIGO_INVALIDO", "El código MFA no es válido"); }
}
