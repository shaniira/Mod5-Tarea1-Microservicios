package com.backendseguros.notification.usecases.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.backendseguros.notification.entities.model.ContactoCliente;
import com.backendseguros.notification.entities.model.Notificacion;
import com.backendseguros.notification.usecases.dto.PolizaEmitidaCommand;
import com.backendseguros.notification.usecases.exception.CanalNoDisponibleException;
import com.backendseguros.notification.usecases.exception.ContactoNoDisponibleException;
import com.backendseguros.notification.usecases.port.out.ContactoClienteRepository;
import com.backendseguros.notification.usecases.port.out.InboxRepository;
import com.backendseguros.notification.usecases.port.out.NotificacionPort;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificarPolizaEmitidaUseCaseTest {
    private final ContactoClienteRepository contactos = mock(ContactoClienteRepository.class);
    private final InboxRepository inbox = mock(InboxRepository.class);
    private final NotificacionPort notificaciones = mock(NotificacionPort.class);
    private final NotificarPolizaEmitidaUseCase useCase =
            new NotificarPolizaEmitidaUseCase(contactos, inbox, notificaciones);

    private final UUID clienteId = UUID.randomUUID();
    private final PolizaEmitidaCommand command =
            new PolizaEmitidaCommand(UUID.randomUUID(), UUID.randomUUID(), clienteId, "POL-2026-0001");

    @Test
    void enviaConElTelefonoDeLaProyeccionYRegistraElEvento() {
        when(contactos.buscar(clienteId))
                .thenReturn(Optional.of(new ContactoCliente(clienteId, "Ana Torres", null, "921175206", 2)));

        var resultado = useCase.execute(command);

        assertThat(resultado).isEqualTo(NotificarPolizaEmitidaUseCase.Resultado.ENVIADA);
        ArgumentCaptor<Notificacion> enviada = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).enviar(enviada.capture());
        assertThat(enviada.getValue().destinatario()).isEqualTo("921175206");
        assertThat(enviada.getValue().contenido()).contains("POL-2026-0001");
        verify(inbox).registrar(command.eventId(), "PolicyIssued");
    }

    @Test
    void unEventoRepetidoNoEnviaElWhatsappDosVeces() {
        when(inbox.fueProcesado(command.eventId())).thenReturn(true);

        var resultado = useCase.execute(command);

        assertThat(resultado).isEqualTo(NotificarPolizaEmitidaUseCase.Resultado.DUPLICADO);
        verifyNoInteractions(notificaciones);
    }

    @Test
    void sinContactoEnLaProyeccionFallaSinEnviar() {
        when(contactos.buscar(clienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ContactoNoDisponibleException.class);
        verifyNoInteractions(notificaciones);
        verify(inbox, never()).registrar(any(), any());
    }

    @Test
    void siElCanalEstaCaidoNoMarcaElEventoComoProcesado() {
        when(contactos.buscar(clienteId))
                .thenReturn(Optional.of(new ContactoCliente(clienteId, "Ana", null, "921175206", 1)));
        doThrow(new CanalNoDisponibleException("caido", null)).when(notificaciones).enviar(any());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(CanalNoDisponibleException.class);
        verify(inbox, never()).registrar(any(), any());
    }
}
