package com.andinaseguros.identity.usecases.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.andinaseguros.identity.entities.enums.RolUsuario;
import com.andinaseguros.identity.entities.model.Usuario;
import com.andinaseguros.identity.usecases.port.out.repository.CorreoClienteRepository;
import com.andinaseguros.identity.usecases.port.out.security.TokenGeneratorPort;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmisorDeTokensTest {
    private final TokenGeneratorPort tokens = mock(TokenGeneratorPort.class);
    private final CorreoClienteRepository correos = mock(CorreoClienteRepository.class);
    private final EmisorDeTokens emisor = new EmisorDeTokens(tokens, correos);

    @Test
    void unClienteLlevaSuCustomerIdBuscadoPorCorreo() {
        UUID clienteId = UUID.randomUUID();
        when(correos.buscarClientePorCorreo("ana@andina.local")).thenReturn(Optional.of(clienteId));

        var identidad = emisor.identidad(usuario("facebook_1", "ana@andina.local", RolUsuario.CLIENTE));

        assertThat(identidad.customerId()).isEqualTo(clienteId.toString());
        assertThat(identidad.rol()).isEqualTo("CLIENTE");
    }

    @Test
    void siNoTieneCorreoSeBuscaPorElUsernameCuandoEsUnCorreo() {
        UUID clienteId = UUID.randomUUID();
        when(correos.buscarClientePorCorreo("luis@andina.local")).thenReturn(Optional.of(clienteId));

        var identidad = emisor.identidad(usuario("luis@andina.local", null, RolUsuario.CLIENTE));

        assertThat(identidad.customerId()).isEqualTo(clienteId.toString());
    }

    @Test
    void elPersonalInternoNoTieneCustomerId() {
        var identidad = emisor.identidad(usuario("admin", "admin@andina.local", RolUsuario.ADMIN));

        assertThat(identidad.customerId()).isNull();
        verifyNoInteractions(correos);
    }

    @Test
    void unClienteSinRegistroDeClienteNoTieneCustomerId() {
        when(correos.buscarClientePorCorreo(any())).thenReturn(Optional.empty());

        assertThat(emisor.identidad(usuario("x@y.com", "x@y.com", RolUsuario.CLIENTE)).customerId()).isNull();
    }

    private static Usuario usuario(String username, String email, RolUsuario rol) {
        return new Usuario(UUID.randomUUID(), username, email, "hash", null, rol, true, null, false);
    }
}
