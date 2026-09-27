package com.andinaseguros.customer.usecases.service.vehiculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.andinaseguros.customer.entities.enums.TipoUso;
import com.andinaseguros.customer.entities.enums.TipoVehiculo;
import com.andinaseguros.customer.entities.event.DomainEvent;
import com.andinaseguros.customer.entities.event.VehiculoRegistradoEvent;
import com.andinaseguros.customer.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.customer.entities.exception.ReglaNegocioException;
import com.andinaseguros.customer.entities.model.Cliente;
import com.andinaseguros.customer.entities.model.Vehiculo;
import com.andinaseguros.customer.entities.valueobject.Placa;
import com.andinaseguros.customer.usecases.dto.CrearVehiculoRequestModel;
import com.andinaseguros.customer.usecases.port.out.event.DomainEventPublisherPort;
import com.andinaseguros.customer.usecases.port.out.repository.ClienteRepository;
import com.andinaseguros.customer.usecases.port.out.repository.VehiculoRepository;
import com.andinaseguros.customer.usecases.support.TransaccionDirecta;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CrearVehiculoUseCaseTest {
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final VehiculoRepository vehiculos = mock(VehiculoRepository.class);
    private final DomainEventPublisherPort eventos = mock(DomainEventPublisherPort.class);
    private final TransaccionDirecta transaccion = new TransaccionDirecta();
    private final CrearVehiculoUseCase useCase =
            new CrearVehiculoUseCase(
                    clientes,
                    vehiculos,
                    eventos,
                    transaccion,
                    () -> Instant.parse("2026-09-27T10:00:00Z"),
                    UUID::randomUUID);
    private final UUID clienteId = UUID.randomUUID();

    @Test
    void registraElVehiculoYPublicaVehicleRegisteredEnLaMismaTransaccion() {
        clienteExiste();
        when(vehiculos.buscarPorPlaca("ABC-123")).thenReturn(Optional.empty());
        when(vehiculos.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var respuesta = useCase.execute(solicitud("abc-123"));

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventos).publicar(captor.capture());
        VehiculoRegistradoEvent evento = (VehiculoRegistradoEvent) captor.getValue();
        assertThat(evento.vehiculoId()).isEqualTo(respuesta.id());
        assertThat(evento.clienteId()).isEqualTo(clienteId);
        assertThat(evento.placa()).isEqualTo("ABC-123");
        assertThat(evento.tipo()).isEqualTo(TipoVehiculo.AUTO);
        assertThat(evento.uso()).isEqualTo(TipoUso.PARTICULAR);
        assertThat(evento.anioFabricacion()).isEqualTo(2022);
        assertThat(transaccion.usos()).isEqualTo(1);
    }

    @Test
    void rechazaUnaPlacaYaRegistradaAunqueVengaEnMinusculas() {
        clienteExiste();
        when(vehiculos.buscarPorPlaca("ABC-123")).thenReturn(Optional.of(mock(Vehiculo.class)));

        assertThatThrownBy(() -> useCase.execute(solicitud("abc-123")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("placa");
        verifyNoInteractions(eventos);
    }

    @Test
    void rechazaUnClienteInexistenteSinPublicarNada() {
        when(clientes.buscarPorId(clienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(solicitud("ABC-123")))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(eventos);
    }

    @Test
    void unVehiculoDeOtroClienteSeRespondeComoNoEncontrado() {
        UUID vehiculoId = UUID.randomUUID();
        when(vehiculos.buscarPorId(vehiculoId))
                .thenReturn(
                        Optional.of(
                                new Vehiculo(
                                        vehiculoId,
                                        UUID.randomUUID(),
                                        new Placa("XYZ-999"),
                                        "Kia",
                                        "Rio",
                                        2021,
                                        TipoVehiculo.AUTO,
                                        TipoUso.PARTICULAR,
                                        "LIMA")));

        assertThatThrownBy(() -> new ObtenerVehiculoClienteUseCase(vehiculos).execute(clienteId, vehiculoId))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private void clienteExiste() {
        when(clientes.buscarPorId(clienteId))
                .thenReturn(
                        Optional.of(
                                new Cliente(
                                        clienteId,
                                        "DNI",
                                        "70000010",
                                        "Ana",
                                        "Torres",
                                        LocalDate.of(1990, 1, 1),
                                        null,
                                        null,
                                        true)));
    }

    private CrearVehiculoRequestModel solicitud(String placa) {
        return new CrearVehiculoRequestModel(
                clienteId, placa, "Toyota", "Yaris", 2022, TipoVehiculo.AUTO, TipoUso.PARTICULAR, "LIMA");
    }
}
