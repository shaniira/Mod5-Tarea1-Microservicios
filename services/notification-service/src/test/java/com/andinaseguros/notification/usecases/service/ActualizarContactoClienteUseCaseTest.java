package com.andinaseguros.notification.usecases.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.andinaseguros.notification.usecases.dto.ContactoClienteCommand;
import com.andinaseguros.notification.usecases.exception.EventoInvalidoException;
import com.andinaseguros.notification.usecases.port.out.ContactoClienteRepository;
import com.andinaseguros.notification.usecases.port.out.InboxRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActualizarContactoClienteUseCaseTest {
    private final ContactoClienteRepository contactos = mock(ContactoClienteRepository.class);
    private final InboxRepository inbox = mock(InboxRepository.class);
    private final ActualizarContactoClienteUseCase useCase =
            new ActualizarContactoClienteUseCase(contactos, inbox);

    @Test
    void aplicaUnaVersionNueva() {
        when(contactos.guardarSiEsMasNuevo(any())).thenReturn(true);

        var resultado = useCase.execute(command(3));

        assertThat(resultado).isEqualTo(ActualizarContactoClienteUseCase.Resultado.APLICADO);
        verify(inbox).registrar(any(), any());
    }

    @Test
    void descartaUnaVersionMasViejaQueLaGuardada() {
        when(contactos.guardarSiEsMasNuevo(any())).thenReturn(false);

        var resultado = useCase.execute(command(1));

        assertThat(resultado).isEqualTo(ActualizarContactoClienteUseCase.Resultado.VERSION_ANTIGUA);
    }

    @Test
    void ignoraUnEventoYaProcesado() {
        ContactoClienteCommand command = command(2);
        when(inbox.fueProcesado(command.eventId())).thenReturn(true);

        var resultado = useCase.execute(command);

        assertThat(resultado).isEqualTo(ActualizarContactoClienteUseCase.Resultado.DUPLICADO);
        verifyNoInteractions(contactos);
    }

    @Test
    void rechazaUnEventoSinVersion() {
        assertThatThrownBy(() -> useCase.execute(command(0)))
                .isInstanceOf(EventoInvalidoException.class);
    }

    private static ContactoClienteCommand command(long version) {
        return new ContactoClienteCommand(
                UUID.randomUUID(),
                "CustomerUpdated",
                UUID.randomUUID(),
                "Ana Torres",
                "ana@andina.local",
                "921175206",
                version);
    }
}
