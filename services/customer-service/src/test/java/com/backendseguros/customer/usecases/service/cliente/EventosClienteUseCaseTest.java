package com.backendseguros.customer.usecases.service.cliente;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.backendseguros.customer.entities.event.ClienteActualizadoEvent;
import com.backendseguros.customer.entities.event.ClienteRegistradoEvent;
import com.backendseguros.customer.entities.event.DomainEvent;
import com.backendseguros.customer.entities.event.VehiculoRegistradoEvent;
import com.backendseguros.customer.entities.enums.TipoUso;
import com.backendseguros.customer.entities.enums.TipoVehiculo;
import com.backendseguros.customer.entities.exception.ReglaNegocioException;
import com.backendseguros.customer.entities.model.Cliente;
import com.backendseguros.customer.entities.model.Vehiculo;
import com.backendseguros.customer.entities.valueobject.Placa;
import com.backendseguros.customer.usecases.dto.ActualizarContactoClienteRequestModel;
import com.backendseguros.customer.usecases.dto.CrearClienteRequestModel;
import com.backendseguros.customer.usecases.port.out.event.DomainEventPublisherPort;
import com.backendseguros.customer.usecases.port.out.id.IdGeneratorPort;
import com.backendseguros.customer.usecases.port.out.repository.ClienteRepository;
import com.backendseguros.customer.usecases.port.out.repository.VehiculoRepository;
import com.backendseguros.customer.usecases.port.out.time.ClockPort;
import com.backendseguros.customer.usecases.support.TransaccionDirecta;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EventosClienteUseCaseTest {
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final VehiculoRepository vehiculos = mock(VehiculoRepository.class);
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
                                "rosa@backendseguros.local",
                                "999888777"));

        ClienteRegistradoEvent evento = (ClienteRegistradoEvent) capturarEvento();
        assertThat(evento.clienteId()).isEqualTo(respuesta.id());
        assertThat(evento.telefono()).isEqualTo("999888777");
        assertThat(evento.version()).isEqualTo(1);
        assertThat(transaccion.usos()).isEqualTo(1);
    }

    @Test
    void actualizarContactoSubeLaVersionYPublicaCustomerUpdated() {
        Cliente actual = cliente("ana@backendseguros.local", "900000001", 3);
        when(clientes.buscarPorId(actual.getId())).thenReturn(Optional.of(actual));
        when(clientes.buscarPorCorreo("ana.nueva@backendseguros.local")).thenReturn(Optional.empty());
        when(clientes.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var useCase = new ActualizarContactoClienteUseCase(clientes, eventos, transaccion, clock, ids);

        useCase.execute(
                new ActualizarContactoClienteRequestModel(
                        actual.getId(), "ana.nueva@backendseguros.local", "900000002"));

        ClienteActualizadoEvent evento = (ClienteActualizadoEvent) capturarEvento();
        assertThat(evento.version()).isEqualTo(4);
        assertThat(evento.correo()).isEqualTo("ana.nueva@backendseguros.local");
        assertThat(evento.telefono()).isEqualTo("900000002");
    }

    @Test
    void actualizarSinCambiosNoPublicaNada() {
        Cliente actual = cliente("ana@backendseguros.local", "900000001", 1);
        when(clientes.buscarPorId(actual.getId())).thenReturn(Optional.of(actual));
        var useCase = new ActualizarContactoClienteUseCase(clientes, eventos, transaccion, clock, ids);

        useCase.execute(
                new ActualizarContactoClienteRequestModel(
                        actual.getId(), "ana@backendseguros.local", "900000001"));

        verify(clientes, never()).guardar(any());
        verifyNoInteractions(eventos);
    }

    @Test
    void rechazaUnCorreoQueUsaOtroCliente() {
        Cliente actual = cliente("ana@backendseguros.local", "900000001", 1);
        Cliente otro = cliente("luis@backendseguros.local", "900000003", 1);
        when(clientes.buscarPorId(actual.getId())).thenReturn(Optional.of(actual));
        when(clientes.buscarPorCorreo("luis@backendseguros.local")).thenReturn(Optional.of(otro));
        var useCase = new ActualizarContactoClienteUseCase(clientes, eventos, transaccion, clock, ids);

        assertThatThrownBy(
                        () ->
                                useCase.execute(
                                        new ActualizarContactoClienteRequestModel(
                                                actual.getId(), "luis@backendseguros.local", "900000001")))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(eventos);
    }

    @Test
    void backfillPublicaUnEventoPorClienteConSuVersionYDespuesLosVehiculos() {
        Cliente a = cliente("a@backendseguros.local", "900000001", 1);
        when(clientes.listar()).thenReturn(List.of(a, cliente("b@backendseguros.local", "900000002", 5)));
        when(vehiculos.listar())
                .thenReturn(
                        List.of(
                                new Vehiculo(
                                        UUID.randomUUID(),
                                        a.getId(),
                                        new Placa("ABC-123"),
                                        "Toyota",
                                        "Yaris",
                                        2022,
                                        TipoVehiculo.AUTO,
                                        TipoUso.PARTICULAR,
                                        "LIMA")));
        var useCase = new PublicarClientesExistentesUseCase(clientes, vehiculos, eventos, clock, ids);

        var resultado = useCase.execute();

        assertThat(resultado.clientesPublicados()).isEqualTo(2);
        assertThat(resultado.vehiculosPublicados()).isEqualTo(1);
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventos, times(3)).publicar(captor.capture());
        assertThat(captor.getAllValues().subList(0, 2))
                .extracting(e -> ((ClienteRegistradoEvent) e).version())
                .containsExactly(1L, 5L);
        assertThat(captor.getAllValues().get(2)).isInstanceOf(VehiculoRegistradoEvent.class);
    }

    @Test
    void elEventoDeClienteLlevaLaFechaDeNacimiento() {
        when(clientes.buscarPorDocumento("70000098")).thenReturn(Optional.empty());
        when(clientes.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var useCase = new CrearClienteUseCase(clientes, eventos, transaccion, clock, ids);

        useCase.execute(
                new CrearClienteRequestModel(
                        "DNI", "70000098", "Rosa", "Quispe", LocalDate.of(1985, 3, 2), null, null));

        assertThat(((ClienteRegistradoEvent) capturarEvento()).fechaNacimiento())
                .isEqualTo(LocalDate.of(1985, 3, 2));
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
