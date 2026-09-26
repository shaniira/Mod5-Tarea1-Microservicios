package com.andinaseguros.usecases.service.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.andinaseguros.entities.enums.RolUsuario;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.model.Cliente;
import com.andinaseguros.entities.model.Usuario;
import com.andinaseguros.usecases.dto.GoogleLoginRequestModel;
import com.andinaseguros.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.usecases.port.out.repository.ClienteRepository;
import com.andinaseguros.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.usecases.port.out.security.AuthenticatedUser;
import com.andinaseguros.usecases.port.out.security.GoogleIdentity;
import com.andinaseguros.usecases.port.out.security.GoogleIdentityVerifierPort;
import com.andinaseguros.usecases.port.out.security.TokenGeneratorPort;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AutenticarConGoogleUseCaseTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final GoogleIdentityVerifierPort verifier = mock(GoogleIdentityVerifierPort.class);
    private final TokenGeneratorPort tokenGenerator = mock(TokenGeneratorPort.class);
    private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
    private final AutenticarConGoogleUseCase useCase =
            new AutenticarConGoogleUseCase(usuarios, clientes, verifier, tokenGenerator, ids);
    private final GoogleLoginRequestModel solicitud = new GoogleLoginRequestModel("id-token");

    private GoogleIdentity identidad(String subject, String email, boolean emailVerified) {
        return new GoogleIdentity(subject, email, emailVerified, "Usuario Demo", "Usuario", "Demo", "https://pic");
    }

    @Test
    void rechazaUnTokenDeGoogleInvalido() {
        when(verifier.verificar("id-token")).thenThrow(new RuntimeException("firma inválida"));

        assertThatThrownBy(() -> useCase.execute(solicitud))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting(e -> ((ReglaNegocioException) e).getCodigo())
                .isEqualTo("GOOGLE_TOKEN_INVALIDO");

        verifyNoInteractions(usuarios, tokenGenerator);
    }

    @Test
    void rechazaUnEmailDeGoogleNoVerificado() {
        when(verifier.verificar("id-token")).thenReturn(identidad("sub-1", "a@x.com", false));

        assertThatThrownBy(() -> useCase.execute(solicitud))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting(e -> ((ReglaNegocioException) e).getCodigo())
                .isEqualTo("GOOGLE_EMAIL_NO_VERIFICADO");

        verifyNoInteractions(usuarios, tokenGenerator);
    }

    @Test
    void autenticaUnUsuarioGoogleExistenteYActivo() {
        var existente =
                new Usuario(
                                UUID.randomUUID(),
                                "a@x.com",
                                "a@x.com",
                                null,
                                "sub-1",
                                RolUsuario.CLIENTE,
                                true,
                                null,
                                false)
                        .conNombre("Usuario", "Demo");
        when(verifier.verificar("id-token")).thenReturn(identidad("sub-1", "a@x.com", true));
        when(usuarios.buscarPorGoogleSubject("sub-1")).thenReturn(Optional.of(existente));
        when(tokenGenerator.generar(new AuthenticatedUser("a@x.com", "CLIENTE")))
                .thenReturn("jwt-andina");
        when(tokenGenerator.expirationSeconds()).thenReturn(28800L);

        var respuesta = useCase.execute(solicitud);

        assertThat(respuesta.token()).isEqualTo("jwt-andina");
        assertThat(respuesta.tipo()).isEqualTo("Bearer");
        assertThat(respuesta.expiraEnSegundos()).isEqualTo(28800L);
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void rechazaUnUsuarioGoogleExistenteInactivo() {
        var inactivo =
                new Usuario(
                                UUID.randomUUID(),
                                "a@x.com",
                                "a@x.com",
                                null,
                                "sub-1",
                                RolUsuario.CLIENTE,
                                false,
                                null,
                                false)
                        .conNombre("Usuario", "Demo");
        when(verifier.verificar("id-token")).thenReturn(identidad("sub-1", "a@x.com", true));
        when(usuarios.buscarPorGoogleSubject("sub-1")).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> useCase.execute(solicitud))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting(e -> ((ReglaNegocioException) e).getCodigo())
                .isEqualTo("USUARIO_INACTIVO");

        verifyNoInteractions(tokenGenerator);
    }

    @Test
    void creaUnUsuarioNuevoConRolClienteCuandoNoExisteNiPorSubNiPorEmail() {
        var nuevoId = UUID.randomUUID();
        when(verifier.verificar("id-token")).thenReturn(identidad("sub-nuevo", "nuevo@x.com", true));
        when(usuarios.buscarPorGoogleSubject("sub-nuevo")).thenReturn(Optional.empty());
        when(usuarios.buscarPorEmail("nuevo@x.com")).thenReturn(Optional.empty());
        when(clientes.buscarPorCorreo("nuevo@x.com")).thenReturn(Optional.of(mock(Cliente.class)));
        when(ids.generar()).thenReturn(nuevoId);
        when(usuarios.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenGenerator.generar(any())).thenReturn("jwt-andina");
        when(tokenGenerator.expirationSeconds()).thenReturn(28800L);

        var respuesta = useCase.execute(solicitud);

        assertThat(respuesta.token()).isEqualTo("jwt-andina");
        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(captor.capture());
        var creado = captor.getValue();
        assertThat(creado.getId()).isEqualTo(nuevoId);
        assertThat(creado.getUsername()).isEqualTo("nuevo@x.com");
        assertThat(creado.getEmail()).isEqualTo("nuevo@x.com");
        assertThat(creado.getGoogleSubject()).isEqualTo("sub-nuevo");
        assertThat(creado.getPasswordHash()).isNull();
        assertThat(creado.getRol()).isEqualTo(RolUsuario.CLIENTE);
        assertThat(creado.isActivo()).isTrue();
    }

    @Test
    void vinculaGoogleAUnaCuentaLocalConContrasenaSinPedirVinculacionManual() {
        var cuentaLocal =
                new Usuario(
                        UUID.randomUUID(),
                        "a@x.com",
                        "a@x.com",
                        "hash-bcrypt",
                        null,
                        RolUsuario.CLIENTE,
                        true,
                        null,
                        false);
        when(verifier.verificar("id-token")).thenReturn(identidad("sub-1", "a@x.com", true));
        when(usuarios.buscarPorGoogleSubject("sub-1")).thenReturn(Optional.empty());
        when(usuarios.buscarPorEmail("a@x.com")).thenReturn(Optional.of(cuentaLocal));
        when(usuarios.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenGenerator.generar(new AuthenticatedUser("a@x.com", "CLIENTE")))
                .thenReturn("jwt-andina");
        when(tokenGenerator.expirationSeconds()).thenReturn(28800L);

        var respuesta = useCase.execute(solicitud);

        assertThat(respuesta.token()).isEqualTo("jwt-andina");
        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(captor.capture());
        var vinculado = captor.getValue();
        assertThat(vinculado.getGoogleSubject()).isEqualTo("sub-1");
        assertThat(vinculado.getPasswordHash()).isEqualTo("hash-bcrypt");
    }

    @Test
    void vinculaAutomaticamenteUnaCuentaStaffProvisionadaSinContrasena() {
        var agenteId = UUID.randomUUID();
        var cuentaProvisionada =
                new Usuario(
                        agenteId,
                        "agente@x.com",
                        "agente@x.com",
                        null,
                        null,
                        RolUsuario.AGENTE,
                        true,
                        null,
                        false);
        when(verifier.verificar("id-token")).thenReturn(identidad("sub-agente", "agente@x.com", true));
        when(usuarios.buscarPorGoogleSubject("sub-agente")).thenReturn(Optional.empty());
        when(usuarios.buscarPorEmail("agente@x.com")).thenReturn(Optional.of(cuentaProvisionada));
        when(usuarios.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenGenerator.generar(new AuthenticatedUser("agente@x.com", "AGENTE")))
                .thenReturn("jwt-andina");
        when(tokenGenerator.expirationSeconds()).thenReturn(28800L);

        var respuesta = useCase.execute(solicitud);

        assertThat(respuesta.token()).isEqualTo("jwt-andina");
        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(captor.capture());
        var vinculado = captor.getValue();
        assertThat(vinculado.getId()).isEqualTo(agenteId);
        assertThat(vinculado.getGoogleSubject()).isEqualTo("sub-agente");
        assertThat(vinculado.getRol()).isEqualTo(RolUsuario.AGENTE);
        verifyNoInteractions(clientes);
    }

    @Test
    void rechazaCuandoElCorreoNoEstaRegistradoComoCliente() {
        when(verifier.verificar("id-token")).thenReturn(identidad("sub-nuevo", "desconocido@x.com", true));
        when(usuarios.buscarPorGoogleSubject("sub-nuevo")).thenReturn(Optional.empty());
        when(usuarios.buscarPorEmail("desconocido@x.com")).thenReturn(Optional.empty());
        when(clientes.buscarPorCorreo("desconocido@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(solicitud))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting(e -> ((ReglaNegocioException) e).getCodigo())
                .isEqualTo("CLIENTE_NO_REGISTRADO");

        verify(usuarios, never()).guardar(any());
        verifyNoInteractions(tokenGenerator);
    }
}
