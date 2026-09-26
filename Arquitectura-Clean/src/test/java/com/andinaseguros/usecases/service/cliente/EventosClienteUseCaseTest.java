package com.andinaseguros.usecases.service.cliente;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.andinaseguros.entities.event.ClienteActualizadoEvent;
import com.andinaseguros.entities.event.ClienteRegistradoEvent;
import com.andinaseguros.entities.event.DomainEvent;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.model.Cliente;
import com.andinaseguros.usecases.dto.ActualizarContactoClienteRequestModel;
import com.andinaseguros.usecases.dto.CrearClienteRequestModel;
import com.andinaseguros.usecases.port.out.event.DomainEventPublisherPort;
import com.andinaseguros.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.usecases.port.out.repository.ClienteRepository;
import com.andinaseguros.usecases.port.out.time.ClockPort;
import com.andinaseguros.usecases.support.TransaccionDirecta;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EventosClienteUseCaseTest {
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final DomainEventPublisherPort eventos = mock(DomainEventPublisherPort.class);
    private final ClockPort clock = () -> Instant.parse("2026-09-25T10:00:00Z");
    private final IdGeneratorPort ids = UUID::randomUUID;
    private final TransaccionDirecta transaccion = new TransaccionDirecta();

    @Test
    void crearClientePublicaCustomerRegisteredDentroDeLaTransaccion() {
        when(clientes.buscarPorDocumento("70000099")).thenReturn(Optional.empty());
        when(clientes.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var useCase = new CrearClienteUseCase(clientes, eventos, transaccion, clock, ids);

        var respuesta =
                useCase.execute(
                        new CrearClienteRequestModel(
                                "DNI",
                                "70000099",
                                "Rosa",
                                "Quispe",
                                LocalDate.of(1990, 1, 1),
                                "rosa@andina.local",
                                "999888777"));

        ClienteRegistradoEvent evento = (ClienteRegistradoEvent) capturarEvento();
        assertThat(evento.clienteId()).isEqualTo(respuesta.id());
        assertThat(evento.telefono()).isEqualTo("999888777");
        assertThat(evento.version()).isEqualTo(1);
        assertThat(transaccion.usos()).isEqualTo(1);
    }

    @Test
    void actualizarContactoSubeLaVersionYPublicaCustomerUpdated() {
        Cliente actual = cliente("ana@andina.local", "900000001", 3);
        when(clientes.buscarPorId(actual.getId())).thenReturn(Optional.of(actual));
        when(clientes.buscarPorCorreo("ana.nueva@andina.local")).thenReturn(Optional.empty());
        when(clientes.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var useCase = new ActualizarContactoClienteUseCase(clientes, eventos, transaccion, clock, ids);

        useCase.execute(
                new ActualizarContactoClienteRequestModel(
                        actual.getId(), "ana.nueva@andina.local", "900000002"));

        ClienteActualizadoEvent evento = (ClienteActualizadoEvent) capturarEvento();
        assertThat(evento.version()).isEqualTo(4);
        assertThat(evento.correo()).isEqualTo("ana.nueva@andina.local");
        assertThat(evento.telefono()).isEqualTo("900000002");
    }

    @Test
    void actualizarSinCambiosNoPublicaNada() {
        Cliente actual = cliente("ana@andina.local", "900000001", 1);
        when(clientes.buscarPorId(actual.getId())).thenReturn(Optional.of(actual));
        var useCase = new ActualizarContactoClienteUseCase(clientes, eventos, transaccion, clock, ids);

        useCase.execute(
                new ActualizarContactoClienteRequestModel(
                        actual.getId(), "ana@andina.local", "900000001"));

        verify(clientes, never()).guardar(any());
        verifyNoInteractions(eventos);
    }

    @Test
    void rechazaUnCorreoQueUsaOtroCliente() {
        Cliente actual = cliente("ana@andina.local", "900000001", 1);
        Cliente otro = cliente("luis@andina.local", "900000003", 1);
        when(clientes.buscarPorId(actual.getId())).thenReturn(Optional.of(actual));
        when(clientes.buscarPorCorreo("luis@andina.local")).thenReturn(Optional.of(otro));
        var useCase = new ActualizarContactoClienteUseCase(clientes, eventos, transaccion, clock, ids);

        assertThatThrownBy(
                        () ->
                                useCase.execute(
                                        new ActualizarContactoClienteRequestModel(
                                                actual.getId(), "luis@andina.local", "900000001")))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(eventos);
    }

    @Test
    void backfillPublicaUnEventoPorClienteConSuVersionActual() {
        when(clientes.listar())
                .thenReturn(
                        List.of(
                                cliente("a@andina.local", "900000001", 1),
                                cliente("b@andina.local", "900000002", 5)));
        var useCase = new PublicarClientesExistentesUseCase(clientes, eventos, clock, ids);

        int publicados = useCase.execute();

        assertThat(publicados).isEqualTo(2);
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventos, times(2)).publicar(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(e -> ((ClienteRegistradoEvent) e).version())
                .containsExactly(1L, 5L);
    }

    private DomainEvent capturarEvento() {
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventos).publicar(captor.capture());
        return captor.getValue();
    }

    private static Cliente cliente(String correo, String telefono, long version) {
        return new Cliente(
                UUID.randomUUID(),
                "DNI",
                "7" + telefono.substring(2),
                "Ana",
                "Torres",
                LocalDate.of(1990, 5, 10),
                correo,
                telefono,
                true,
                version);
    }
}
