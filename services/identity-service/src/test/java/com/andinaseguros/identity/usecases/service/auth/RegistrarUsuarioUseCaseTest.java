package com.andinaseguros.identity.usecases.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.andinaseguros.identity.entities.enums.RolUsuario;
import com.andinaseguros.identity.entities.exception.ReglaNegocioException;
import com.andinaseguros.identity.entities.model.Usuario;
import com.andinaseguros.identity.usecases.dto.CrearUsuarioRequestModel;
import com.andinaseguros.identity.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.identity.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.identity.usecases.port.out.security.PasswordEncoderPort;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Riesgo S1: nadie puede darse un rol de personal registrándose por su cuenta. */
class RegistrarUsuarioUseCaseTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoderPort encoder = mock(PasswordEncoderPort.class);
    private final IdGeneratorPort ids = UUID::randomUUID;
    private final RegistrarUsuarioUseCase useCase = new RegistrarUsuarioUseCase(usuarios, encoder, ids);

    @Test
    void unRegistroPublicoPidiendoAdminEsRechazado() {
        assertThatThrownBy(
                        () -> useCase.execute(new CrearUsuarioRequestModel("x", "clave", RolUsuario.ADMIN), false))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting(e -> ((ReglaNegocioException) e).getCodigo())
                .isEqualTo("ROL_NO_PERMITIDO");
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void unRegistroPublicoSinRolQuedaComoCliente() {
        when(usuarios.buscarPorUsername("nuevo")).thenReturn(Optional.empty());

        useCase.execute(new CrearUsuarioRequestModel("nuevo", "clave", null), false);

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(guardado.capture());
        assertThat(guardado.getValue().getRol()).isEqualTo(RolUsuario.CLIENTE);
    }

    @Test
    void unAdminAutenticadoPuedeCrearPersonal() {
        when(usuarios.buscarPorUsername("agente2")).thenReturn(Optional.empty());

        useCase.execute(new CrearUsuarioRequestModel("agente2", "clave", RolUsuario.AGENTE), true);

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(guardado.capture());
        assertThat(guardado.getValue().getRol()).isEqualTo(RolUsuario.AGENTE);
    }
}
