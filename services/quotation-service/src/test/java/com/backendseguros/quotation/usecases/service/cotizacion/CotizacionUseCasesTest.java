package com.backendseguros.quotation.usecases.service.cotizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.backendseguros.quotation.entities.enums.EstadoCotizacion;
import com.backendseguros.quotation.entities.enums.EstadoTablaTarifaria;
import com.backendseguros.quotation.entities.enums.TipoUso;
import com.backendseguros.quotation.entities.enums.TipoVehiculo;
import com.backendseguros.quotation.entities.event.CotizacionAceptadaEvent;
import com.backendseguros.quotation.entities.event.DomainEvent;
import com.backendseguros.quotation.entities.exception.RecursoNoEncontradoException;
import com.backendseguros.quotation.entities.exception.ReglaNegocioException;
import com.backendseguros.quotation.entities.model.ClienteRef;
import com.backendseguros.quotation.entities.model.Cotizacion;
import com.backendseguros.quotation.entities.model.TablaTarifaria;
import com.backendseguros.quotation.entities.model.VehiculoRef;
import com.backendseguros.quotation.entities.service.MotorDeTarificacion;
import com.backendseguros.quotation.entities.valueobject.Dinero;
import com.backendseguros.quotation.entities.valueobject.PeriodoVigencia;
import com.backendseguros.quotation.usecases.dto.CrearCotizacionRequestModel;
import com.backendseguros.quotation.usecases.exception.ClientesNoDisponiblesException;
import com.backendseguros.quotation.usecases.port.out.cliente.DirectorioClientesPort;
import com.backendseguros.quotation.usecases.port.out.event.DomainEventPublisherPort;
import com.backendseguros.quotation.usecases.port.out.repository.CotizacionRepository;
import com.backendseguros.quotation.usecases.port.out.repository.ReferenciaClientesRepository;
import com.backendseguros.quotation.usecases.port.out.repository.TablaTarifariaRepository;
import com.backendseguros.quotation.usecases.support.TransaccionDirecta;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Criterios de salida de la fase 5: cotizar desde las proyecciones y publicar quote.accepted.v1. */
class CotizacionUseCasesTest {
    private final ReferenciaClientesRepository referencias = mock(ReferenciaClientesRepository.class);
    private final DirectorioClientesPort directorio = mock(DirectorioClientesPort.class);
    private final TablaTarifariaRepository tablas = mock(TablaTarifariaRepository.class);
    private final CotizacionRepository cotizaciones = mock(CotizacionRepository.class);
    private final DomainEventPublisherPort eventos = mock(DomainEventPublisherPort.class);
    private final CrearCotizacionUseCase crear =
            new CrearCotizacionUseCase(referencias, directorio, tablas, cotizaciones, new MotorDeTarificacion());

    private final UUID clienteId = UUID.randomUUID();
    private final UUID vehiculoId = UUID.randomUUID();
    private final ClienteRef cliente = new ClienteRef(clienteId, LocalDate.of(1990, 1, 15), true);
    private final VehiculoRef vehiculo =
            new VehiculoRef(vehiculoId, clienteId, TipoVehiculo.AUTO, TipoUso.PARTICULAR, 2023);

    @Test
    void conClienteYVehiculoEnLasProyeccionesCotizaSinLlamarACustomerService() {
        when(referencias.buscarCliente(clienteId)).thenReturn(Optional.of(cliente));
        when(referencias.buscarVehiculo(vehiculoId)).thenReturn(Optional.of(vehiculo));
        tablaVigente();
        when(cotizaciones.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var respuesta = crear.execute(solicitud());

        assertThat(respuesta.estado()).isEqualTo(EstadoCotizacion.VIGENTE);
        // 1250 de prima base + 10 % de gastos + 3 % de recargo, sin factores.
        assertThat(respuesta.prima()).isEqualByComparingTo("1412.50");
        assertThat(respuesta.expira()).isEqualTo(respuesta.creada().plusDays(15));
        verifyNoInteractions(directorio);
    }

    @Test
    void siFaltaEnLaProyeccionLoTraeDeCustomerServiceYLoGuardaConVersionCero() {
        when(referencias.buscarCliente(clienteId)).thenReturn(Optional.empty());
        when(referencias.buscarVehiculo(vehiculoId)).thenReturn(Optional.of(vehiculo));
        when(directorio.buscarCliente(clienteId)).thenReturn(Optional.of(cliente));
        tablaVigente();
        when(cotizaciones.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        crear.execute(solicitud());

        verify(referencias).guardarClienteSiEsMasNuevo(cliente, 0);
    }

    @Test
    void unClienteSinFechaDeNacimientoEnLaProyeccionSeCompletaConCustomerService() {
        when(referencias.buscarCliente(clienteId)).thenReturn(Optional.of(new ClienteRef(clienteId, null, true)));
        when(referencias.buscarVehiculo(vehiculoId)).thenReturn(Optional.of(vehiculo));
        when(directorio.buscarCliente(clienteId)).thenReturn(Optional.of(cliente));
        tablaVigente();
        when(cotizaciones.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        crear.execute(solicitud());

        verify(directorio).buscarCliente(clienteId);
        verify(referencias).completarFechaNacimiento(cliente);
        verify(referencias, never()).guardarClienteSiEsMasNuevo(any(), eq(0L));
    }

    @Test
    void conCustomerServiceCaidoYSinDatosLocalesResponde503() {
        when(referencias.buscarCliente(clienteId)).thenReturn(Optional.empty());
        when(directorio.buscarCliente(clienteId))
                .thenThrow(new ClientesNoDisponiblesException(new RuntimeException("timeout")));

        assertThatThrownBy(() -> crear.execute(solicitud())).isInstanceOf(ClientesNoDisponiblesException.class);
        verifyNoInteractions(cotizaciones);
    }

    @Test
    void unClienteQueNoExisteEnNingunLadoEs404() {
        when(referencias.buscarCliente(clienteId)).thenReturn(Optional.empty());
        when(directorio.buscarCliente(clienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> crear.execute(solicitud())).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void unVehiculoDeOtroClienteSeRechazaComoEnElMonolito() {
        when(referencias.buscarCliente(clienteId)).thenReturn(Optional.of(cliente));
        when(referencias.buscarVehiculo(vehiculoId))
                .thenReturn(Optional.of(new VehiculoRef(vehiculoId, UUID.randomUUID(), TipoVehiculo.AUTO, TipoUso.PARTICULAR, 2023)));

        assertThatThrownBy(() -> crear.execute(solicitud()))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no pertenece");
    }

    @Test
    void aceptarPublicaQuoteAcceptedEnLaMismaTransaccion() {
        TransaccionDirecta transaccion = new TransaccionDirecta();
        var aceptar =
                new AceptarCotizacionUseCase(
                        cotizaciones, eventos, transaccion, () -> Instant.parse("2026-09-27T10:00:00Z"), UUID::randomUUID);
        Cotizacion vigente = cotizacion(EstadoCotizacion.VIGENTE);
        when(cotizaciones.buscarPorId(vigente.getId())).thenReturn(Optional.of(vigente));
        when(cotizaciones.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var respuesta = aceptar.execute(vigente.getId());

        assertThat(respuesta.estado()).isEqualTo(EstadoCotizacion.ACEPTADA);
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventos).publicar(captor.capture());
        CotizacionAceptadaEvent evento = (CotizacionAceptadaEvent) captor.getValue();
        assertThat(evento.cotizacionId()).isEqualTo(vigente.getId());
        assertThat(evento.clienteId()).isEqualTo(vigente.getClienteId());
        assertThat(evento.prima()).isEqualByComparingTo("1200");
        assertThat(evento.expira()).isEqualTo(vigente.getFechaExpiracion());
        assertThat(transaccion.usos()).isEqualTo(1);
    }

    @Test
    void policyIssuedMarcaLaCotizacionEmitidaUnaSolaVez() {
        var marcar = new MarcarCotizacionEmitidaUseCase(cotizaciones);
        Cotizacion aceptada = cotizacion(EstadoCotizacion.ACEPTADA);
        when(cotizaciones.buscarPorId(aceptada.getId())).thenReturn(Optional.of(aceptada));

        assertThat(marcar.execute(aceptada.getId())).isEqualTo(MarcarCotizacionEmitidaUseCase.Resultado.MARCADA);
        assertThat(aceptada.getEstado()).isEqualTo(EstadoCotizacion.EMITIDA);
        assertThat(marcar.execute(aceptada.getId())).isEqualTo(MarcarCotizacionEmitidaUseCase.Resultado.YA_EMITIDA);
        verify(cotizaciones, times(1)).guardar(any());
        assertThat(marcar.execute(UUID.randomUUID())).isEqualTo(MarcarCotizacionEmitidaUseCase.Resultado.NO_EXISTE);
    }

    @Test
    void laCompensacionDeLaSagaDejaLaCotizacionAceptadaYRegistraElMotivo() {
        var rechazos = mock(com.backendseguros.quotation.usecases.port.out.repository.RechazoEmisionRepository.class);
        var registrar = new RegistrarRechazoEmisionUseCase(cotizaciones, rechazos);
        Cotizacion aceptada = cotizacion(EstadoCotizacion.ACEPTADA);
        when(cotizaciones.buscarPorId(aceptada.getId())).thenReturn(Optional.of(aceptada));
        UUID eventId = UUID.randomUUID();

        var resultado = registrar.execute(eventId, aceptada.getId(), "COTIZACION_VENCIDA", "venció");

        assertThat(resultado).isEqualTo(RegistrarRechazoEmisionUseCase.Resultado.REGISTRADO);
        assertThat(aceptada.getEstado()).isEqualTo(EstadoCotizacion.ACEPTADA);
        verify(rechazos).registrar(eventId, aceptada.getId(), "COTIZACION_VENCIDA", "venció");
        verify(cotizaciones, never()).guardar(any());
    }

    @Test
    void pendientesDeEmisionSonLasAceptadas() {
        when(cotizaciones.listarPorEstado(EstadoCotizacion.ACEPTADA)).thenReturn(List.of(cotizacion(EstadoCotizacion.ACEPTADA)));

        assertThat(new ListarCotizacionesPendientesEmisionUseCase(cotizaciones).execute()).hasSize(1);
    }

    private void tablaVigente() {
        when(tablas.buscarVigente(eq(TipoVehiculo.AUTO), eq(TipoUso.PARTICULAR), any()))
                .thenReturn(
                        Optional.of(
                                new TablaTarifaria(
                                        UUID.randomUUID(),
                                        "DEMO-AUTO",
                                        1,
                                        TipoVehiculo.AUTO,
                                        TipoUso.PARTICULAR,
                                        Dinero.soles(new BigDecimal("1250")),
                                        Dinero.soles(new BigDecimal("900")),
                                        new PeriodoVigencia(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)),
                                        "NT",
                                        EstadoTablaTarifaria.VIGENTE,
                                        List.of())));
    }

    private CrearCotizacionRequestModel solicitud() {
        return new CrearCotizacionRequestModel(clienteId, vehiculoId, 0, null, null, null);
    }

    private static Cotizacion cotizacion(EstadoCotizacion estado) {
        return new Cotizacion(
                UUID.randomUUID(),
                "COT-TEST",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Dinero.soles(new BigDecimal("1200")),
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(10),
                estado);
    }
}
