package com.backendseguros.identity.usecases.service.auth;

import com.backendseguros.identity.usecases.port.out.repository.CorreoClienteRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.backendseguros.identity.entities.enums.RolUsuario;
import com.backendseguros.identity.entities.exception.ReglaNegocioException;
import com.backendseguros.identity.entities.model.Usuario;
import com.backendseguros.identity.usecases.port.out.facebook.FacebookOAuthPort;
import com.backendseguros.identity.usecases.port.out.facebook.OAuthStatePort;
import com.backendseguros.identity.usecases.port.out.id.IdGeneratorPort;
import com.backendseguros.identity.usecases.port.out.repository.UsuarioRepository;
import com.backendseguros.identity.usecases.port.out.security.SecretEncryptionPort;
import com.backendseguros.identity.usecases.port.out.security.TokenGeneratorPort;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AutenticarConFacebookUseCaseTest {
    @Test
    void iniciaOAuthYCreaUsuarioConTokenCifrado() {
        var states = mock(OAuthStatePort.class);
        var facebook = mock(FacebookOAuthPort.class);
        var users = mock(UsuarioRepository.class);
        var ids = mock(IdGeneratorPort.class);
        var encryption = mock(SecretEncryptionPort.class);
        var tokens = mock(TokenGeneratorPort.class);
        var useCase = new AutenticarConFacebookUseCase(states, facebook, users, ids, encryption, new EmisorDeTokens(tokens, mock(CorreoClienteRepository.class)));
        var identity = new FacebookOAuthPort.FacebookIdentity("123", "user@example.com", "User", "User", "Example", "facebook-token", Instant.now().plusSeconds(3600), Set.of("email"));
        when(states.create()).thenReturn("state");
        when(facebook.authorizationUrl("state")).thenReturn("https://facebook.example/oauth");
        when(states.consume("state")).thenReturn(true);
        when(facebook.exchangeCode("code")).thenReturn(identity);
        when(users.buscarPorProveedorYProveedorUsuarioId("FACEBOOK", "123")).thenReturn(Optional.empty());
        when(users.buscarPorUsername("facebook_123")).thenReturn(Optional.empty());
        when(users.buscarPorUsername("user@example.com")).thenReturn(Optional.empty());
        when(ids.generar()).thenReturn(UUID.randomUUID());
        when(encryption.encrypt("facebook-token")).thenReturn("encrypted-token");
        when(users.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokens.generar(any())).thenReturn("jwt");
        when(tokens.expirationSeconds()).thenReturn(3600L);

        assertThat(useCase.iniciar()).isEqualTo("https://facebook.example/oauth");
        assertThat(useCase.callback("code", "state").token()).isEqualTo("jwt");
        verify(encryption).encrypt("facebook-token");
        verify(users).guardar(any(Usuario.class));
    }

    @Test
    void rechazaCallbackConStateInvalido() {
        var states = mock(OAuthStatePort.class);
        var useCase = new AutenticarConFacebookUseCase(states, mock(FacebookOAuthPort.class), mock(UsuarioRepository.class), mock(IdGeneratorPort.class), mock(SecretEncryptionPort.class), mock(EmisorDeTokens.class));
        when(states.consume("bad")).thenReturn(false);

        assertThatThrownBy(() -> useCase.callback("code", "bad"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Respuesta de Facebook inválida");
    }
}