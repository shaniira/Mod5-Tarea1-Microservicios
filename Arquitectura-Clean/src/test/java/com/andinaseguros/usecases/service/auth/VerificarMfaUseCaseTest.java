package com.andinaseguros.usecases.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.andinaseguros.entities.enums.RolUsuario;
import com.andinaseguros.entities.model.Usuario;
import com.andinaseguros.usecases.dto.MfaVerifyRequestModel;
import com.andinaseguros.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.usecases.port.out.security.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerificarMfaUseCaseTest {
    @Test
    void emiteJwtDespuesDeValidarTotp() {
        var usuarios = mock(UsuarioRepository.class); var challenges = mock(MfaChallengePort.class);
        var totp = mock(TotpVerifierPort.class); var tokens = mock(TokenGeneratorPort.class);
        var identity = new AuthenticatedUser("admin", "ADMIN");
        var usuario = new Usuario(UUID.randomUUID(), "admin", null, "hash", null, RolUsuario.ADMIN, true, "SECRET", true);
        when(challenges.consumir("challenge")).thenReturn(identity);
        when(usuarios.buscarPorUsername("admin")).thenReturn(Optional.of(usuario));
        when(totp.verificar("SECRET", "123456")).thenReturn(true);
        when(tokens.generar(identity)).thenReturn("jwt"); when(tokens.expirationSeconds()).thenReturn(3600L);

        var response = new VerificarMfaUseCase(usuarios, challenges, totp, tokens)
                .execute(new MfaVerifyRequestModel("challenge", "123456"));
        assertThat(response.token()).isEqualTo("jwt");
    }
}
