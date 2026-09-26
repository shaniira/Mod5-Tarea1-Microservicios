package com.andinaseguros.usecases.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.andinaseguros.entities.enums.RolUsuario;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.model.Usuario;
import com.andinaseguros.usecases.port.out.facebook.FacebookOAuthPort;
import com.andinaseguros.usecases.port.out.facebook.OAuthStatePort;
import com.andinaseguros.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.usecases.port.out.security.SecretEncryptionPort;
import com.andinaseguros.usecases.port.out.security.TokenGeneratorPort;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FacebookLoginUseCaseScenariosTest {
    @Test
    void usuarioNuevoConTokenValidoSeRegistraYCreaJwt() {
        var fixture = fixture();
        stubValidCallbackForNewUser(fixture);

        var response = fixture.useCase.callback("code", "state");

        assertThat(response.token()).isEqualTo("jwt");
        assertThat(response.tipo()).isEqualTo("Bearer");
        verify(fixture.encryption).encrypt("facebook-token");
        verify(fixture.users).guardar(any(Usuario.class));
        verify(fixture.tokens).generar(argThat(user -> user.username().equals("facebook_123")));
    }

    @Test
    void usuarioExistenteActualizaTokenYEmiteJwt() {
        var fixture = fixture();
        var existing =
                new Usuario(
                        UUID.randomUUID(),
                        "facebook_123",
                        "user@example.com",
                        null,
                        null,
                        RolUsuario.CLIENTE,
                        true,
                        null,
                        false,
                        "FACEBOOK",
                        "123",
                        "old-token",
                        1,
                        "email");
        when(fixture.states.consume("state")).thenReturn(true);
        when(fixture.facebook.exchangeCode("code")).thenReturn(identity());
        when(fixture.users.buscarPorProveedorYProveedorUsuarioId("FACEBOOK", "123"))
                .thenReturn(Optional.of(existing));
        when(fixture.encryption.encrypt("facebook-token")).thenReturn("encrypted-token");
        when(fixture.users.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fixture.tokens.generar(any())).thenReturn("jwt-existing");
        when(fixture.tokens.expirationSeconds()).thenReturn(1800L);

        assertThat(fixture.useCase.callback("code", "state").token()).isEqualTo("jwt-existing");
        verify(fixture.users, never()).buscarPorUsername(any());
        verify(fixture.ids, never()).generar();
    }

    @Test
    void tokenInvalidoSinIdentidadEsRechazado() {
        var fixture = fixture();
        when(fixture.states.consume("state")).thenReturn(true);
        when(fixture.facebook.exchangeCode("code"))
                .thenReturn(new FacebookOAuthPort.FacebookIdentity("", null, null, null, null, "token", Instant.now(), Set.of("public_profile")));

        assertThatThrownBy(() -> fixture.useCase.callback("code", "state"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Facebook no devolvió una identidad válida");
        verifyNoInteractions(fixture.users, fixture.tokens);
    }

    @Test
    void permisosInsuficientesSonRechazados() {
        var fixture = fixture();
        when(fixture.states.consume("state")).thenReturn(true);
        when(fixture.facebook.exchangeCode("code"))
                .thenReturn(new FacebookOAuthPort.FacebookIdentity("123", null, null, null, null, "token", Instant.now(), Set.of()));

        assertThatThrownBy(() -> fixture.useCase.callback("code", "state"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Facebook no concedió los permisos necesarios");
    }

    @Test
    void cancelacionNoCanjeaCodigoNiCreaSesion() {
        var fixture = fixture();

        assertThatThrownBy(() -> fixture.useCase.callback(null, "state"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Respuesta de Facebook inválida");
        verifyNoInteractions(fixture.facebook, fixture.users, fixture.tokens);
    }

    @Test
    void errorDelProveedorNoCreaSesion() {
        var fixture = fixture();
        when(fixture.states.consume("state")).thenReturn(true);
        when(fixture.facebook.exchangeCode("code"))
                .thenThrow(new ReglaNegocioException("FACEBOOK_NO_DISPONIBLE", "No fue posible validar Facebook"));

        assertThatThrownBy(() -> fixture.useCase.callback("code", "state"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No fue posible validar Facebook");
        verifyNoInteractions(fixture.tokens);
    }

    private void stubValidCallbackForNewUser(Fixture fixture) {
        when(fixture.states.consume("state")).thenReturn(true);
        when(fixture.facebook.exchangeCode("code")).thenReturn(identity());
        when(fixture.users.buscarPorProveedorYProveedorUsuarioId("FACEBOOK", "123"))
                .thenReturn(Optional.empty());
        when(fixture.users.buscarPorUsername("facebook_123")).thenReturn(Optional.empty());
        when(fixture.users.buscarPorUsername("user@example.com")).thenReturn(Optional.empty());
        when(fixture.ids.generar()).thenReturn(UUID.randomUUID());
        when(fixture.encryption.encrypt("facebook-token")).thenReturn("encrypted-token");
        when(fixture.users.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fixture.tokens.generar(any())).thenReturn("jwt");
        when(fixture.tokens.expirationSeconds()).thenReturn(3600L);
    }

    private FacebookOAuthPort.FacebookIdentity identity() {
        return new FacebookOAuthPort.FacebookIdentity("123", "user@example.com", "User", "User", "Example",
                "facebook-token", Instant.now().plusSeconds(3600), Set.of("email", "public_profile"));
    }

    private Fixture fixture() {
        var states = mock(OAuthStatePort.class);
        var facebook = mock(FacebookOAuthPort.class);
        var users = mock(UsuarioRepository.class);
        var ids = mock(IdGeneratorPort.class);
        var encryption = mock(SecretEncryptionPort.class);
        var tokens = mock(TokenGeneratorPort.class);
        return new Fixture(states, facebook, users, ids, encryption, tokens,
                new AutenticarConFacebookUseCase(states, facebook, users, ids, encryption, tokens));
    }

    private record Fixture(
            OAuthStatePort states,
            FacebookOAuthPort facebook,
            UsuarioRepository users,
            IdGeneratorPort ids,
            SecretEncryptionPort encryption,
            TokenGeneratorPort tokens,
            AutenticarConFacebookUseCase useCase) {}
}