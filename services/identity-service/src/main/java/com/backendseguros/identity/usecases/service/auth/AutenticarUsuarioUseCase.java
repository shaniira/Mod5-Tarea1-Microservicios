package com.backendseguros.identity.usecases.service.auth;

import com.backendseguros.identity.usecases.dto.LoginRequestModel;
import com.backendseguros.identity.usecases.dto.Responses.TokenResponse;
import com.backendseguros.identity.usecases.dto.Responses.ResultadoLogin;
import com.backendseguros.identity.entities.exception.ReglaNegocioException;
import com.backendseguros.identity.usecases.port.out.repository.UsuarioRepository;
import com.backendseguros.identity.usecases.port.out.security.*;

public class AutenticarUsuarioUseCase {
    private final UsuarioRepository usuarios;
    private final PasswordEncoderPort passwordEncoder;
    private final EmisorDeTokens emisor;
    private final MfaChallengePort mfaChallenges;

    public AutenticarUsuarioUseCase(
            UsuarioRepository usuarios,
            PasswordEncoderPort passwordEncoder,
            EmisorDeTokens emisor,
            MfaChallengePort mfaChallenges) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.emisor = emisor;
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
        var identity = emisor.identidad(usuario);
        if (usuario.isMfaHabilitado()) {
            var challenge = mfaChallenges.crear(identity);
            return ResultadoLogin.requiereMfa(challenge.token(), challenge.expiraEnSegundos());
        }
        return ResultadoLogin.exitoso(
                emisor.emitir(identity));
    }

    private ReglaNegocioException credencialesInvalidas() {
        return new ReglaNegocioException("CREDENCIALES_INVALIDAS", "Credenciales inválidas");
    }
}
