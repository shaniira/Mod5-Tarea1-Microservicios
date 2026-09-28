package com.backendseguros.policy.usecases.service.poliza;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.backendseguros.policy.entities.enums.EstadoPoliza;
import com.backendseguros.policy.entities.event.DomainEvent;
import com.backendseguros.policy.entities.event.EmisionRechazadaEvent;
import com.backendseguros.policy.entities.event.PolizaEmitidaEvent;
import com.backendseguros.policy.entities.exception.ReglaNegocioException;
import com.backendseguros.policy.entities.model.CotizacionAceptada;
import com.backendseguros.policy.entities.model.Poliza;
import com.backendseguros.policy.entities.valueobject.Dinero;
import com.backendseguros.policy.entities.valueobject.PeriodoVigencia;
import com.backendseguros.policy.usecases.dto.EmitirPolizaRequestModel;
import com.backendseguros.policy.usecases.exception.CotizacionYaEmitidaException;
import com.backendseguros.policy.usecases.port.out.event.DomainEventPublisherPort;
import com.backendseguros.policy.usecases.port.out.repository.CotizacionesAceptadasRepository;
import com.backendseguros.policy.usecases.port.out.repository.PolizaRepository;
import com.backendseguros.policy.usecases.support.TransaccionDirecta;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Saga de emisión (pasos 6.3 y 6.4). */
class EmitirPolizaUseCaseTest {
    private final CotizacionesAceptadasRepository cotizaciones = mock(CotizacionesAceptadasRepository.class);
    private final PolizaRepository polizas = mock(PolizaRepository.class);
    private final DomainEventPublisherPort eventos = mock(DomainEventPublisherPort.class);
    private final TransaccionDirecta transaccion = new TransaccionDirecta();
    private final Instant ahora = Instant.parse("2026-09-27T10:00:00Z");
    private final EmitirPolizaUseCase emitir =
            new EmitirPolizaUseCase(cotizaciones, polizas, eventos, transaccion, () -> ahora, UUID::randomUUID);

    private final UUID cotizacionId = UUID.randomUUID();
    private final UUID clienteId = UUID.randomUUID();
    private final UUID vehiculoId = UUID.randomUUID();

    @Test
    void emiteUnaPolizaVigentePorUnAnioYPublicaPolicyIssued() {
        aceptada(LocalDateTime.of(2026, 10, 10, 0, 0));
        when(polizas.buscarPorCotizacionId(cotizacionId)).thenReturn(Optional.empty());
        when(polizas.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var poliza = emitir.execute(new EmitirPolizaRequestModel(cotizacionId, LocalDate.of(2026, 10, 1)));

        assertThat(poliza.estado()).isEqualTo(EstadoPoliza.VIGENTE);
        assertThat(poliza.clienteId()).isEqualTo(clienteId);
        assertThat(poliza.prima()).isEqualByComparingTo("1412.50");
        assertThat(poliza.fin()).isEqualTo(LocalDate.of(2027, 10, 1));
        assertThat(poliza.numero()).startsWith("POL-2026-");
        PolizaEmitidaEvent evento = (PolizaEmitidaEvent) unicoEvento();
        assertThat(evento.cotizacionId()).isEqualTo(cotizacionId);
        assertThat(evento.version()).isEqualTo(1);
        assertThat(transaccion.usos()).isEqualTo(1);
    }

    @Test
    void sinCotizacionAceptadaNoEmiteComoElMonolito() {
        when(cotizaciones.buscar(cotizacionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emitir.execute(new EmitirPolizaRequestModel(cotizacionId, LocalDate.now())))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("aceptada");
        verifyNoInteractions(eventos);
    }

    @Test
    void unaCotizacionYaEmitidaSeRechazaYPublicaLaCompensacion() {
        aceptada(LocalDateTime.of(2026, 10, 10, 0, 0));
        Poliza existente = poliza();
        when(polizas.buscarPorCotizacionId(cotizacionId)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> emitir.execute(new EmitirPolizaRequestModel(cotizacionId, LocalDate.now())))
                .isInstanceOf(CotizacionYaEmitidaException.class);
        EmisionRechazadaEvent evento = (EmisionRechazadaEvent) unicoEvento();
        assertThat(evento.codigoMotivo()).isEqualTo("COTIZACION_YA_EMITIDA");
        assertThat(evento.polizaExistenteId()).isEqualTo(existente.getId());
        verify(polizas, never()).guardar(any());
    }

    @Test
    void unaCotizacionVencidaSeRechazaYPublicaLaCompensacion() {
        aceptada(LocalDateTime.of(2026, 9, 20, 0, 0));
        when(polizas.buscarPorCotizacionId(cotizacionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emitir.execute(new EmitirPolizaRequestModel(cotizacionId, LocalDate.now())))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("venció");
        assertThat(((EmisionRechazadaEvent) unicoEvento()).codigoMotivo()).isEqualTo("COTIZACION_VENCIDA");
    }

    @Test
    void siDosEmisionesChocanEnElIndiceUnicoLaSegundaSeRechaza() {
        aceptada(LocalDateTime.of(2026, 10, 10, 0, 0));
        when(polizas.buscarPorCotizacionId(cotizacionId)).thenReturn(Optional.empty());
        when(polizas.guardar(any())).thenThrow(new CotizacionYaEmitidaException());

        assertThatThrownBy(() -> emitir.execute(new EmitirPolizaRequestModel(cotizacionId, LocalDate.now())))
                .isInstanceOf(CotizacionYaEmitidaException.class);
        List<DomainEvent> publicados = eventosPublicados();
        assertThat(publicados).hasSize(1).first().isInstanceOf(EmisionRechazadaEvent.class);
    }

    @Test
    void unConflictoTransitorioDeMongoSeReintentaYTerminaEnYaEmitida() {
        aceptada(LocalDateTime.of(2026, 10, 10, 0, 0));
        Poliza ganadora = poliza();
        // 1er intento: nadie la emitió todavía; al guardar, MongoDB informa WriteConflict (la otra
        // solicitud está escribiendo). 2do intento: ya existe la póliza de la otra solicitud.
        when(polizas.buscarPorCotizacionId(cotizacionId)).thenReturn(Optional.empty(), Optional.of(ganadora));
        when(polizas.guardar(any())).thenThrow(new RuntimeException("Command failed with error 112 (WriteConflict)"));

        assertThatThrownBy(() -> emitir.execute(new EmitirPolizaRequestModel(cotizacionId, LocalDate.now())))
                .isInstanceOf(CotizacionYaEmitidaException.class);
        EmisionRechazadaEvent evento = (EmisionRechazadaEvent) unicoEvento();
        assertThat(evento.polizaExistenteId()).isEqualTo(ganadora.getId());
        verify(polizas, times(1)).guardar(any());
    }

    @Test
    void unErrorQueNoSeResuelveConReintentosSePropaga() {
        aceptada(LocalDateTime.of(2026, 10, 10, 0, 0));
        when(polizas.buscarPorCotizacionId(cotizacionId)).thenReturn(Optional.empty());
        when(polizas.guardar(any())).thenThrow(new IllegalStateException("MongoDB caído"));

        assertThatThrownBy(() -> emitir.execute(new EmitirPolizaRequestModel(cotizacionId, LocalDate.now())))
                .isInstanceOf(IllegalStateException.class);
        verify(polizas, times(EmitirPolizaUseCase.MAX_INTENTOS)).guardar(any());
    }

    @Test
    void unaCotizacionAceptadaSinFechaDeExpiracionNoSeAdmite() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> new CotizacionAceptada(
                                cotizacionId, "COT-1", clienteId, vehiculoId, Dinero.soles(BigDecimal.TEN), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void aceptada(LocalDateTime expira) {
        when(cotizaciones.buscar(cotizacionId))
                .thenReturn(
                        Optional.of(
                                new CotizacionAceptada(
                                        cotizacionId, "COT-1", clienteId, vehiculoId,
                                        Dinero.soles(new BigDecimal("1412.50")), expira)));
    }

    private Poliza poliza() {
        return new Poliza(
                UUID.randomUUID(), "POL-1", cotizacionId, clienteId, vehiculoId, Dinero.soles(BigDecimal.TEN),
                new PeriodoVigencia(LocalDate.now(), LocalDate.now().plusYears(1)), EstadoPoliza.VIGENTE, null, 1);
    }

    private DomainEvent unicoEvento() {
        List<DomainEvent> publicados = eventosPublicados();
        assertThat(publicados).hasSize(1);
        return publicados.get(0);
    }

    private List<DomainEvent> eventosPublicados() {
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventos, atLeast(0)).publicar(captor.capture());
        return captor.getAllValues();
    }
}
