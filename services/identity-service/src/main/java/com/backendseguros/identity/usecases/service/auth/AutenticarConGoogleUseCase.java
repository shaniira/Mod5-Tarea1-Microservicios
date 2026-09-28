package com.backendseguros.identity.usecases.service.auth;

import com.backendseguros.identity.entities.enums.RolUsuario;
import com.backendseguros.identity.entities.exception.ReglaNegocioException;
import com.backendseguros.identity.usecases.exception.ProveedorIdentidadNoDisponibleException;
import com.backendseguros.identity.entities.model.Usuario;
import com.backendseguros.identity.usecases.dto.GoogleLoginRequestModel;
import com.backendseguros.identity.usecases.dto.Responses.TokenResponse;
import com.backendseguros.identity.usecases.port.out.id.IdGeneratorPort;
import com.backendseguros.identity.usecases.port.out.repository.CorreoClienteRepository;
import com.backendseguros.identity.usecases.port.out.repository.UsuarioRepository;
import com.backendseguros.identity.usecases.port.out.security.*;

public class AutenticarConGoogleUseCase {
    private final UsuarioRepository usuarios;
    private final CorreoClienteRepository correosClientes;
    private final GoogleIdentityVerifierPort googleIdentityVerifier;
    private final EmisorDeTokens emisor;
    private final IdGeneratorPort ids;

    public AutenticarConGoogleUseCase(
            UsuarioRepository usuarios,
            CorreoClienteRepository correosClientes,
            GoogleIdentityVerifierPort googleIdentityVerifier,
            EmisorDeTokens emisor,
            IdGeneratorPort ids) {
        this.usuarios = usuarios;
        this.correosClientes = correosClientes;
        this.googleIdentityVerifier = googleIdentityVerifier;
        this.emisor = emisor;
        this.ids = ids;
    }

    public TokenResponse execute(GoogleLoginRequestModel solicitud) {
        GoogleIdentity identidad = verificar(solicitud.idToken());

        if (!identidad.emailVerified()) {
            throw emailNoVerificado();
        }

        Usuario usuario =
                usuarios.buscarPorGoogleSubject(identidad.subject())
                        .map(existente -> actualizarNombreSiCambio(existente, identidad))
                        .orElseGet(() -> vincularOCrearUsuario(identidad));

        if (!usuario.isActivo()) {
            throw usuarioInactivo();
        }

        var identity = emisor.identidad(usuario);
        return emisor.emitir(identity);
    }

    private GoogleIdentity verificar(String idToken) {
        try {
            return googleIdentityVerifier.verificar(idToken);
        } catch (ProveedorIdentidadNoDisponibleException e) {
            throw new ReglaNegocioException(
                    "GOOGLE_NO_DISPONIBLE",
                    "No podemos validar tu cuenta de Google en este momento. Entra con tu"
                            + " contraseña o inténtalo más tarde.");
        } catch (RuntimeException e) {
            throw tokenInvalido();
        }
    }

    private Usuario vincularOCrearUsuario(GoogleIdentity identidad) {
        var existente = usuarios.buscarPorEmail(identidad.email());
        if (existente.isPresent()) {
            return vincularGoogleAUsuarioExistente(existente.get(), identidad);
        }
        if (correosClientes.buscarClientePorCorreo(identidad.email()).isEmpty()) {
            throw clienteNoRegistrado();
        }
        return usuarios.guardar(
                new Usuario(
                        ids.generar(),
                        identidad.email(),
                        identidad.email(),
                        null,
                        identidad.subject(),
                        RolUsuario.CLIENTE,
                        true,
                        null,
                        false)
                        .conNombre(identidad.givenName(), identidad.familyName()));
    }

    private Usuario vincularGoogleAUsuarioExistente(Usuario existente, GoogleIdentity identidad) {
        return usuarios.guardar(
                existente
                        .conGoogleSubject(identidad.subject())
                        .conNombre(identidad.givenName(), identidad.familyName()));
    }

    private Usuario actualizarNombreSiCambio(Usuario usuario, GoogleIdentity identidad) {
        if (identidad.givenName() == null || identidad.givenName().isBlank()) {
            return usuario;
        }
        boolean sinCambios =
                identidad.givenName().equals(usuario.getNombres())
                        && java.util.Objects.equals(identidad.familyName(), usuario.getApellidos());
        return sinCambios ? usuario : usuarios.guardar(usuario.conNombre(identidad.givenName(), identidad.familyName()));
    }

    private ReglaNegocioException tokenInvalido() {
        return new ReglaNegocioException(
                "GOOGLE_TOKEN_INVALIDO", "No se pudo validar el token de Google");
    }

    private ReglaNegocioException emailNoVerificado() {
        return new ReglaNegocioException(
                "GOOGLE_EMAIL_NO_VERIFICADO", "El correo de Google no está verificado");
    }

    private ReglaNegocioException usuarioInactivo() {
        return new ReglaNegocioException("USUARIO_INACTIVO", "El usuario está inactivo");
    }

    private ReglaNegocioException clienteNoRegistrado() {
        return new ReglaNegocioException(
                "CLIENTE_NO_REGISTRADO",
                "Tu correo no está registrado como cliente de Backend Seguros. Contacta a un agente"
                        + " para registrarte.");
    }
}
