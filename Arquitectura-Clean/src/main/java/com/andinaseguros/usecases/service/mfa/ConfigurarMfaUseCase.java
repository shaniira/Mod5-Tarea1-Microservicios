package com.andinaseguros.usecases.service.mfa;

import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.usecases.dto.Responses.MfaSetupResponse;
import com.andinaseguros.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.usecases.port.out.security.MfaSecretGeneratorPort;
import com.andinaseguros.usecases.port.out.security.QrCodeGeneratorPort;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ConfigurarMfaUseCase {
    private final UsuarioRepository usuarios;
    private final MfaSecretGeneratorPort secrets;
    private final QrCodeGeneratorPort qr;

    public ConfigurarMfaUseCase(UsuarioRepository usuarios, MfaSecretGeneratorPort secrets, QrCodeGeneratorPort qr) {
        this.usuarios = usuarios;
        this.secrets = secrets;
        this.qr = qr;
    }

    public MfaSetupResponse execute(String username) {
        var usuario = usuarios.buscarPorUsername(username).orElseThrow(this::usuarioNoEncontrado);
        if (usuario.isMfaHabilitado()) throw error("MFA_YA_HABILITADO", "MFA ya está habilitado");
        String secret = secrets.generar();
        usuarios.guardar(usuario.conMfa(secret, false));
        String label = encode("Andina Seguros:" + username);
        String issuer = encode("Andina Seguros");
        String uri = "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + issuer
                + "&algorithm=SHA1&digits=6&period=30";
        return new MfaSetupResponse(secret, uri, qr.generarDataUri(uri));
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    private ReglaNegocioException usuarioNoEncontrado() { return error("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"); }
    private ReglaNegocioException error(String codigo, String mensaje) { return new ReglaNegocioException(codigo, mensaje); }
}
