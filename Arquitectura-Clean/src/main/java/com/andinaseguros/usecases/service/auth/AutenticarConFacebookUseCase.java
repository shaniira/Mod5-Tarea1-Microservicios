package com.andinaseguros.usecases.service.auth;

import com.andinaseguros.entities.enums.RolUsuario;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.model.Usuario;
import com.andinaseguros.usecases.dto.Responses.TokenResponse;
import com.andinaseguros.usecases.port.out.facebook.FacebookOAuthPort;
import com.andinaseguros.usecases.port.out.facebook.OAuthStatePort;
import com.andinaseguros.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.usecases.port.out.security.AuthenticatedUser;
import com.andinaseguros.usecases.port.out.security.SecretEncryptionPort;
import com.andinaseguros.usecases.port.out.security.TokenGeneratorPort;

public class AutenticarConFacebookUseCase {
    private static final String FACEBOOK = "FACEBOOK";
    private final OAuthStatePort states;
    private final FacebookOAuthPort facebook;
    private final UsuarioRepository usuarios;
    private final IdGeneratorPort ids;
    private final SecretEncryptionPort encryption;
    private final TokenGeneratorPort tokens;

    public AutenticarConFacebookUseCase(
            OAuthStatePort states, FacebookOAuthPort facebook, UsuarioRepository usuarios,
            IdGeneratorPort ids, SecretEncryptionPort encryption, TokenGeneratorPort tokens) {
        this.states = states;
        this.facebook = facebook;
        this.usuarios = usuarios;
        this.ids = ids;
        this.encryption = encryption;
        this.tokens = tokens;
    }

    public String iniciar() {
        return facebook.authorizationUrl(states.create());
    }

    public TokenResponse callback(String code, String state) {
        if (code == null || code.isBlank() || !states.consume(state)) {
            throw new ReglaNegocioException("FACEBOOK_CALLBACK_INVALIDO", "Respuesta de Facebook inválida");
        }
        var identity = facebook.exchangeCode(code);
        if (identity.id() == null || identity.id().isBlank()) {
            throw new ReglaNegocioException("FACEBOOK_IDENTIDAD_INVALIDA", "Facebook no devolvió una identidad válida");
        }
        if (identity.scopes() == null || identity.scopes().isEmpty()) {
            throw new ReglaNegocioException(
                    "FACEBOOK_PERMISOS_INSUFICIENTES",
                    "Facebook no concedió los permisos necesarios");
        }
        var usuario = usuarios.buscarPorProveedorYProveedorUsuarioId(FACEBOOK, identity.id())
            .map(existing -> actualizarAutorizacion(existing, identity))
            .orElseGet(() -> crearUsuario(identity));
        if (!usuario.isActivo()) {
            throw new ReglaNegocioException("CREDENCIALES_INVALIDAS", "Credenciales inválidas");
        }
        return new TokenResponse(tokens.generar(new AuthenticatedUser(usuario.getUsername(), usuario.getRol().name())),
                "Bearer", tokens.expirationSeconds());
    }

    private Usuario crearUsuario(FacebookOAuthPort.FacebookIdentity identity) {
        String username = "facebook_" + identity.id();
        if (usuarios.buscarPorUsername(username).isPresent()) {
            throw new ReglaNegocioException("USUARIO_DUPLICADO", "No fue posible vincular la cuenta de Facebook");
        }
        if (identity.email() != null && usuarios.buscarPorUsername(identity.email()).isPresent()) {
            throw new ReglaNegocioException("VINCULACION_REQUIERE_CONFIRMACION", "La cuenta requiere vinculación confirmada");
        }
        var usuario =
            new Usuario(
                ids.generar(),
                username,
                identity.email(),
                null,
                null,
                RolUsuario.CLIENTE,
                true,
                null,
                false,
                FACEBOOK,
                identity.id(),
                encryption.encrypt(identity.accessToken()),
                identity.expiresAt() == null ? 0 : identity.expiresAt().getEpochSecond(),
                String.join(",", identity.scopes()))
                .conNombre(identity.firstName(), identity.lastName());
        return usuarios.guardar(usuario);
    }

    private Usuario actualizarAutorizacion(
            Usuario usuario, FacebookOAuthPort.FacebookIdentity identity) {
        var actualizado =
                identity.email() != null && !identity.email().isBlank()
                        ? usuario.conEmail(identity.email())
                        : usuario;
        actualizado =
                identity.firstName() != null && !identity.firstName().isBlank()
                        ? actualizado.conNombre(identity.firstName(), identity.lastName())
                        : actualizado;
        return usuarios.guardar(
                actualizado.conAutorizacionFacebook(
                        encryption.encrypt(identity.accessToken()),
                        identity.expiresAt() == null ? 0 : identity.expiresAt().getEpochSecond(),
                        String.join(",", identity.scopes())));
    }
}