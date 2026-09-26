package com.andinaseguros.usecases.service.auth;

import com.andinaseguros.usecases.dto.LoginRequestModel;
import com.andinaseguros.usecases.dto.Responses.TokenResponse;
import com.andinaseguros.usecases.dto.Responses.ResultadoLogin;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.usecases.port.out.security.*;

public class AutenticarUsuarioUseCase {
    private final UsuarioRepository usuarios;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenGeneratorPort tokenGenerator;
    private final MfaChallengePort mfaChallenges;

    public AutenticarUsuarioUseCase(
            UsuarioRepository usuarios,
            PasswordEncoderPort passwordEncoder,
            TokenGeneratorPort tokenGenerator,
            MfaChallengePort mfaChallenges) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.mfaChallenges = mfaChallenges;
    }

    public ResultadoLogin execute(LoginRequestModel solicitud) {
        var usuario =
                usuarios.buscarPorUsername(solicitud.username())
                        .orElseThrow(this::credencialesInvalidas);
        if (!usuario.isActivo()
                || usuario.getPasswordHash() == null
                || !passwordEncoder.coincide(solicitud.password(), usuario.getPasswordHash())) {
            throw credencialesInvalidas();
        }
        var identity = new AuthenticatedUser(usuario.getUsername(), usuario.getRol().name());
        if (usuario.isMfaHabilitado()) {
            var challenge = mfaChallenges.crear(identity);
            return ResultadoLogin.requiereMfa(challenge.token(), challenge.expiraEnSegundos());
        }
        return ResultadoLogin.exitoso(
                new TokenResponse(
                        tokenGenerator.generar(identity),
                        "Bearer",
                        tokenGenerator.expirationSeconds()));
    }

    private ReglaNegocioException credencialesInvalidas() {
        return new ReglaNegocioException("CREDENCIALES_INVALIDAS", "Credenciales inválidas");
    }
}
