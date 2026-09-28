package com.backendseguros.policy.usecases.service.renovacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.backendseguros.policy.entities.enums.EstadoPoliza;
import com.backendseguros.policy.entities.enums.EstadoRenovacion;
import com.backendseguros.policy.entities.event.DomainEvent;
import com.backendseguros.policy.entities.event.PolizaRenovadaEvent;
import com.backendseguros.policy.entities.exception.ReglaNegocioException;
import com.backendseguros.policy.entities.model.Poliza;
import com.backendseguros.policy.entities.model.PropuestaRenovacion;
import com.backendseguros.policy.entities.model.SiniestroRef;
import com.backendseguros.policy.entities.service.CalculadorPrimaRenovacion;
import com.backendseguros.policy.entities.service.EvaluadorRenovacion;
import com.backendseguros.policy.entities.service.PoliticaVariacionPrima;
import com.backendseguros.policy.entities.valueobject.Dinero;
import com.backendseguros.policy.entities.valueobject.PeriodoVigencia;
import com.backendseguros.policy.usecases.port.out.event.DomainEventPublisherPort;
import com.backendseguros.policy.usecases.port.out.repository.PolizaRepository;
import com.backendseguros.policy.usecases.port.out.repository.RenovacionRepository;
import com.backendseguros.policy.usecases.port.out.repository.SincronizacionSiniestrosPort;
import com.backendseguros.policy.usecases.port.out.repository.SiniestrosRefRepository;
import com.backendseguros.policy.usecases.port.out.siniestros.HistorialSiniestrosPort;
import com.backendseguros.policy.usecases.exception.SiniestrosNoDisponiblesException;
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

/** Paso 6.5: renovaciones con el historial de claim_ref y policy.renewed.v1. */
class RenovacionUseCasesTest {
    private final PolizaRepository polizas = mock(PolizaRepository.class);
    private final SiniestrosRefRepository siniestros = mock(SiniestrosRefRepository.class);
    private final SincronizacionSiniestrosPort sincronizacion = mock(SincronizacionSiniestrosPort.class);
    private final RenovacionRepository renovaciones = mock(RenovacionRepository.class);
    private final DomainEventPublisherPort eventos = mock(DomainEventPublisherPort.class);
    private final HistorialSiniestrosPort historial = mock(HistorialSiniestrosPort.class);
    private final EvaluarRenovacionUseCase evaluar =
            new EvaluarRenovacionUseCase(
                    polizas, siniestros, sincronizacion, renovaciones, new EvaluadorRenovacion(), new CalculadorPrimaRenovacion(),
                    new PoliticaVariacionPrima());
    private final Poliza poliza =
            new Poliza(
                    UUID.randomUUID(), "POL-2026-1", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    Dinero.soles(new BigDecimal("1000")),
                    new PeriodoVigencia(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1)), EstadoPoliza.VIGENTE, null, 1);

    @Test
    void conSiniestrosResponsablesCerradosRecalculaComoElMonolito() {
        when(polizas.buscarPorId(poliza.getId())).thenReturn(Optional.of(poliza));
        when(sincronizacion.estaAlDia()).thenReturn(true);
        when(siniestros.listarPorPoliza(poliza.getId()))
                .thenReturn(List.of(ref(false, true), ref(false, true), ref(false, false)));
        when(renovaciones.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var propuesta = evaluar.execute(poliza.getId());

        assertThat(propuesta.estado()).isEqualTo(EstadoRenovacion.REQUIERE_RECALCULO);
        assertThat(propuesta.nuevaPrima()).isEqualByComparingTo("1203.93");
        assertThat(propuesta.siniestrosConsiderados()).isEqualTo(3);
        assertThat(propuesta.motivo()).isEqualTo("2 siniestro(s) responsable(s): se recalculó la prima");
    }

    @Test
    void conUnSiniestroAbiertoSeBloquea() {
        when(polizas.buscarPorId(poliza.getId())).thenReturn(Optional.of(poliza));
        when(sincronizacion.estaAlDia()).thenReturn(true);
        when(siniestros.listarPorPoliza(poliza.getId())).thenReturn(List.of(ref(true, false)));

        assertThatThrownBy(() -> evaluar.execute(poliza.getId()))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo")
                .isEqualTo("SINIESTROS_PENDIENTES");
        verify(renovaciones, never()).guardar(any());
    }

    @Test
    void sinLaCargaInicialDeSiniestrosSeTrataComoPendiente() {
        when(polizas.buscarPorId(poliza.getId())).thenReturn(Optional.of(poliza));
        when(sincronizacion.estaAlDia()).thenReturn(false);

        assertThatThrownBy(() -> evaluar.execute(poliza.getId()))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo")
                .isEqualTo("SINIESTROS_PENDIENTES");
        verify(siniestros, never()).listarPorPoliza(any());
    }

    @Test
    void generarLaPolizaRenovadaPublicaPolicyRenewedEnLaMismaTransaccion() {
        TransaccionDirecta transaccion = new TransaccionDirecta();
        var generar =
                new GenerarPolizaRenovadaUseCase(
                        renovaciones, polizas, eventos, transaccion, () -> Instant.parse("2026-12-20T10:00:00Z"),
                        UUID::randomUUID, historial);
        LocalDateTime ahora = LocalDateTime.now();
        PropuestaRenovacion propuesta =
                new PropuestaRenovacion(
                        UUID.randomUUID(), poliza.getId(), poliza.getPrima(), Dinero.soles(new BigDecimal("1080")),
                        new BigDecimal("8"), 1, EstadoRenovacion.ACEPTADA, "ok", ahora, ahora.plusDays(30), ahora, null);
        when(renovaciones.buscarPorId(propuesta.id())).thenReturn(Optional.of(propuesta));
        when(polizas.buscarPorId(poliza.getId())).thenReturn(Optional.of(poliza));
        when(polizas.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        // claims-service confirma el mismo historial que se evaluó: un siniestro cerrado.
        when(historial.listarPorPoliza(poliza.getId())).thenReturn(List.of(ref(false, true)));

        var nueva = generar.execute(propuesta.id());

        assertThat(nueva.inicio()).isEqualTo(LocalDate.of(2027, 1, 2));
        assertThat(nueva.estado()).isEqualTo(EstadoPoliza.VIGENTE);
        assertThat(nueva.renovacionOrigenId()).isEqualTo(propuesta.id());
        assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.RENOVADA);
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventos).publicar(captor.capture());
        PolizaRenovadaEvent evento = (PolizaRenovadaEvent) captor.getValue();
        assertThat(evento.polizaAnteriorId()).isEqualTo(poliza.getId());
        assertThat(evento.polizaNuevaId()).isEqualTo(nueva.id());
        assertThat(evento.versionPolizaAnterior()).isEqualTo(2);
        assertThat(transaccion.usos()).isEqualTo(1);
    }

    private SiniestroRef ref(boolean abierto, boolean responsable) {
        return new SiniestroRef(UUID.randomUUID(), poliza.getId(), abierto, responsable);
    }

    /** Fase 7: al generar se confirma con claims-service; un siniestro abierto bloquea la renovación. */
    @Test
    void siClaimsConfirmaUnSiniestroAbiertoNoGeneraLaPoliza() {
        PropuestaRenovacion propuesta = propuestaAceptada(1);
        when(historial.listarPorPoliza(poliza.getId())).thenReturn(List.of(ref(true, false)));

        assertThatThrownBy(() -> generador().execute(propuesta.id()))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo")
                .isEqualTo("SINIESTROS_PENDIENTES");
        assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.VIGENTE);
        verifyNoInteractions(eventos);
    }

    @Test
    void siElHistorialCambioDesdeLaEvaluacionPideEvaluarDeNuevo() {
        PropuestaRenovacion propuesta = propuestaAceptada(1);
        when(historial.listarPorPoliza(poliza.getId())).thenReturn(List.of(ref(false, true), ref(false, true)));

        assertThatThrownBy(() -> generador().execute(propuesta.id()))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo")
                .isEqualTo("RENOVACION_DESACTUALIZADA");
        verifyNoInteractions(eventos);
    }

    @Test
    void siClaimsNoRespondeNoRenuevaYPideReintentar() {
        PropuestaRenovacion propuesta = propuestaAceptada(1);
        when(historial.listarPorPoliza(poliza.getId()))
                .thenThrow(new SiniestrosNoDisponiblesException(new RuntimeException("timeout")));

        assertThatThrownBy(() -> generador().execute(propuesta.id())).isInstanceOf(SiniestrosNoDisponiblesException.class);
        assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.VIGENTE);
        verify(polizas, never()).guardar(any());
        verifyNoInteractions(eventos);
    }

    private GenerarPolizaRenovadaUseCase generador() {
        return new GenerarPolizaRenovadaUseCase(
                renovaciones, polizas, eventos, new TransaccionDirecta(), () -> Instant.parse("2026-12-20T10:00:00Z"),
                UUID::randomUUID, historial);
    }

    private PropuestaRenovacion propuestaAceptada(int siniestrosConsiderados) {
        LocalDateTime ahora = LocalDateTime.now();
        PropuestaRenovacion propuesta =
                new PropuestaRenovacion(
                        UUID.randomUUID(), poliza.getId(), poliza.getPrima(), Dinero.soles(new BigDecimal("1080")),
                        new BigDecimal("8"), siniestrosConsiderados, EstadoRenovacion.ACEPTADA, "ok", ahora,
                        ahora.plusDays(30), ahora, null);
        when(renovaciones.buscarPorId(propuesta.id())).thenReturn(Optional.of(propuesta));
        when(polizas.buscarPorId(poliza.getId())).thenReturn(Optional.of(poliza));
        return propuesta;
    }
}
