package com.backendseguros.identity.usecases.service.auth;

import com.backendseguros.identity.usecases.port.out.repository.CorreoClienteRepository;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.backendseguros.identity.entities.enums.RolUsuario;
import com.backendseguros.identity.entities.exception.ReglaNegocioException;
import com.backendseguros.identity.entities.model.Usuario;
import com.backendseguros.identity.usecases.dto.LoginRequestModel;
import com.backendseguros.identity.usecases.port.out.repository.UsuarioRepository;
import com.backendseguros.identity.usecases.port.out.security.AuthenticatedUser;
import com.backendseguros.identity.usecases.port.out.security.PasswordEncoderPort;
import com.backendseguros.identity.usecases.port.out.security.TokenGeneratorPort;
import com.backendseguros.identity.usecases.port.out.security.MfaChallengePort;
import com.backendseguros.identity.usecases.port.out.security.MfaChallenge;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AutenticarUsuarioUseCaseTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoderPort passwordEncoder = mock(PasswordEncoderPort.class);
    private final TokenGeneratorPort tokenGenerator = mock(TokenGeneratorPort.class);
    private final MfaChallengePort challenges = mock(MfaChallengePort.class);
    private final AutenticarUsuarioUseCase useCase =
            new AutenticarUsuarioUseCase(usuarios, passwordEncoder, new EmisorDeTokens(tokenGenerator, mock(CorreoClienteRepository.class)), challenges);
    private final LoginRequestModel solicitud = new LoginRequestModel("admin", "Admin123*");

    @Test
    void autenticaUnUsuarioLocalConCredencialesCorrectas() {
        var usuario =
                new Usuario(
                        UUID.randomUUID(),
                        "admin",
                        null,
                        "hash-bcrypt",
                        null,
                        RolUsuario.ADMIN,
                        true,
                        null,
                        false);
        when(usuarios.buscarPorUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.coincide("Admin123*", "hash-bcrypt")).thenReturn(true);
        when(tokenGenerator.generar(new AuthenticatedUser("admin", "ADMIN")))
                .thenReturn("jwt-emitido");
        when(tokenGenerator.expirationSeconds()).thenReturn(28800L);

        var respuesta = useCase.execute(solicitud);

        assertThat(respuesta.requiresMfa()).isFalse();
        assertThat(respuesta.token().token()).isEqualTo("jwt-emitido");
        assertThat(respuesta.token().tipo()).isEqualTo("Bearer");
    }

    @Test
    void solicitaSegundoFactorCuandoMfaEstaHabilitado() {
        var usuario = new Usuario(UUID.randomUUID(), "admin", null, "hash-bcrypt", null,
                RolUsuario.ADMIN, true, "SECRET", true);
        when(usuarios.buscarPorUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.coincide("Admin123*", "hash-bcrypt")).thenReturn(true);
        when(challenges.crear(new AuthenticatedUser("admin", "ADMIN")))
                .thenReturn(new MfaChallenge("challenge", 300));

        var respuesta = useCase.execute(solicitud);

        assertThat(respuesta.requiresMfa()).isTrue();
        assertThat(respuesta.challengeToken()).isEqualTo("challenge");
        assertThat(respuesta.token()).isNull();
        verifyNoInteractions(tokenGenerator);
    }

    @Test
    void rechazaUnUsuarioInexistente() {
        when(usuarios.buscarPorUsername("admin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(solicitud))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCodigo())
                        .isEqualTo("CREDENCIALES_INVALIDAS"));

        verifyNoInteractions(passwordEncoder, tokenGenerator);
    }

    @Test
    void rechazaUnUsuarioInactivo() {
        var usuario =
                new Usuario(
                        UUID.randomUUID(),
                        "admin",
                        null,
                        "hash-bcrypt",
                        null,
                        RolUsuario.ADMIN,
                        false,
                        null,
                        false);
        when(usuarios.buscarPorUsername("admin")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> useCase.execute(solicitud))
                .isInstanceOf(ReglaNegocioException.class);

        verifyNoInteractions(passwordEncoder, tokenGenerator);
    }

    @Test
    void rechazaUnaContrasenaIncorrecta() {
        var usuario =
                new Usuario(
                        UUID.randomUUID(),
                        "admin",
                        null,
                        "hash-bcrypt",
                        null,
                        RolUsuario.ADMIN,
                        true,
                        null,
                        false);
        when(usuarios.buscarPorUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.coincide("Admin123*", "hash-bcrypt")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(solicitud))
                .isInstanceOf(ReglaNegocioException.class);

        verifyNoInteractions(tokenGenerator);
    }

    @Test
    void rechazaUnUsuarioSoloGoogleSinLanzarNullPointerException() {
        var usuarioSoloGoogle =
                new Usuario(
                        UUID.randomUUID(),
                        "usuario@gmail.com",
                        "usuario@gmail.com",
                        null,
                        "sub-1",
                        RolUsuario.CLIENTE,
                        true,
                        null,
                        false);
        when(usuarios.buscarPorUsername("admin")).thenReturn(Optional.of(usuarioSoloGoogle));

        assertThatThrownBy(() -> useCase.execute(solicitud))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCodigo())
                        .isEqualTo("CREDENCIALES_INVALIDAS"));

        verifyNoInteractions(passwordEncoder, tokenGenerator);
    }
}
