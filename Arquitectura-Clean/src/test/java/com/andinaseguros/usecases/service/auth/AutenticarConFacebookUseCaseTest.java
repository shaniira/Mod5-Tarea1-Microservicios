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

class AutenticarConFacebookUseCaseTest {
    @Test
    void iniciaOAuthYCreaUsuarioConTokenCifrado() {
        var states = mock(OAuthStatePort.class);
        var facebook = mock(FacebookOAuthPort.class);
        var users = mock(UsuarioRepository.class);
        var ids = mock(IdGeneratorPort.class);
        var encryption = mock(SecretEncryptionPort.class);
        var tokens = mock(TokenGeneratorPort.class);
        var useCase = new AutenticarConFacebookUseCase(states, facebook, users, ids, encryption, tokens);
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
        var useCase = new AutenticarConFacebookUseCase(states, mock(FacebookOAuthPort.class), mock(UsuarioRepository.class), mock(IdGeneratorPort.class), mock(SecretEncryptionPort.class), mock(TokenGeneratorPort.class));
        when(states.consume("bad")).thenReturn(false);

        assertThatThrownBy(() -> useCase.callback("code", "bad"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Respuesta de Facebook inválida");
    }
}