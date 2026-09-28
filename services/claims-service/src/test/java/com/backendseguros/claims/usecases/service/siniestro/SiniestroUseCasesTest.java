package com.backendseguros.claims.usecases.service.siniestro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.backendseguros.claims.entities.enums.EstadoPoliza;
import com.backendseguros.claims.entities.enums.EstadoSiniestro;
import com.backendseguros.claims.entities.event.DomainEvent;
import com.backendseguros.claims.entities.event.SiniestroEstadoCambiadoEvent;
import com.backendseguros.claims.entities.event.SiniestroRegistradoEvent;
import com.backendseguros.claims.entities.exception.RecursoNoEncontradoException;
import com.backendseguros.claims.entities.exception.ReglaNegocioException;
import com.backendseguros.claims.entities.model.PolizaRef;
import com.backendseguros.claims.entities.model.Siniestro;
import com.backendseguros.claims.entities.valueobject.Dinero;
import com.backendseguros.claims.usecases.dto.RegistrarSiniestroRequestModel;
import com.backendseguros.claims.usecases.port.out.event.DomainEventPublisherPort;
import com.backendseguros.claims.usecases.port.out.repository.PolizaRefRepository;
import com.backendseguros.claims.usecases.port.out.repository.SiniestroRepository;
import com.backendseguros.claims.usecases.port.out.time.ClockPort;
import com.backendseguros.claims.usecases.support.TransaccionDirecta;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Criterio de salida de la fase 4: registrar y cambiar el estado sin la base de pólizas. */
class SiniestroUseCasesTest {
    private final PolizaRefRepository polizas = mock(PolizaRefRepository.class);
    private final SiniestroRepository siniestros = mock(SiniestroRepository.class);
    private final DomainEventPublisherPort eventos = mock(DomainEventPublisherPort.class);
    private final TransaccionDirecta transaccion = new TransaccionDirecta();
    private final ClockPort clock = () -> Instant.parse("2026-09-27T10:00:00Z");
    private final UUID polizaId = UUID.randomUUID();
    private final UUID clienteId = UUID.randomUUID();

    private final RegistrarSiniestroUseCase registrar =
            new RegistrarSiniestroUseCase(polizas, siniestros, eventos, transaccion, clock, UUID::randomUUID);
    private final ActualizarEstadoSiniestroUseCase actualizar =
            new ActualizarEstadoSiniestroUseCase(polizas, siniestros, eventos, transaccion, clock, UUID::randomUUID);

    @Test
    void registraEnUnaPolizaVigenteDeLaProyeccionYPublicaClaimRegistered() {
        poliza(EstadoPoliza.VIGENTE);
        when(siniestros.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var respuesta = registrar.execute(solicitud());

        SiniestroRegistradoEvent evento = (SiniestroRegistradoEvent) capturarEvento();
        assertThat(evento.siniestroId()).isEqualTo(respuesta.id());
        assertThat(evento.polizaId()).isEqualTo(polizaId);
        assertThat(evento.clienteId()).isEqualTo(clienteId);
        assertThat(evento.abierto()).isTrue();
        assertThat(evento.responsabilidadAsegurado()).isTrue();
        assertThat(evento.version()).isEqualTo(1);
        assertThat(transaccion.usos()).isEqualTo(1);
    }

    @Test
    void siLaProyeccionNoTieneLaPolizaSeRechazaSinConsultarANadieMas() {
        when(polizas.buscarPorId(polizaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrar.execute(solicitud()))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(siniestros, eventos);
    }

    @Test
    void rechazaUnaPolizaNoVigenteComoElMonolito() {
        poliza(EstadoPoliza.VENCIDA);

        assertThatThrownBy(() -> registrar.execute(solicitud()))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("vigentes");
        verifyNoInteractions(eventos);
    }

    @Test
    void cambiarEstadoSubeLaVersionYPublicaClaimStatusChanged() {
        poliza(EstadoPoliza.VIGENTE);
        Siniestro siniestro = siniestro(EstadoSiniestro.REPORTADO);
        when(siniestros.buscarPorId(siniestro.id())).thenReturn(Optional.of(siniestro));
        when(siniestros.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var respuesta = actualizar.execute(polizaId, siniestro.id(), EstadoSiniestro.LIQUIDADO);

        assertThat(respuesta.estado()).isEqualTo(EstadoSiniestro.LIQUIDADO);
        SiniestroEstadoCambiadoEvent evento = (SiniestroEstadoCambiadoEvent) capturarEvento();
        assertThat(evento.estadoAnterior()).isEqualTo(EstadoSiniestro.REPORTADO);
        assertThat(evento.estadoNuevo()).isEqualTo(EstadoSiniestro.LIQUIDADO);
        assertThat(evento.abierto()).isFalse();
        assertThat(evento.version()).isEqualTo(2);
    }

    @Test
    void unSiniestroCerradoNoCambiaNiPublica() {
        poliza(EstadoPoliza.VIGENTE);
        Siniestro siniestro = siniestro(EstadoSiniestro.RECHAZADO);
        when(siniestros.buscarPorId(siniestro.id())).thenReturn(Optional.of(siniestro));

        assertThatThrownBy(() -> actualizar.execute(polizaId, siniestro.id(), EstadoSiniestro.APROBADO))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(eventos);
    }

    @Test
    void unSiniestroDeOtraPolizaSeRechaza() {
        UUID otraPoliza = UUID.randomUUID();
        when(polizas.buscarPorId(otraPoliza))
                .thenReturn(Optional.of(new PolizaRef(otraPoliza, clienteId, "POL-2", EstadoPoliza.VIGENTE)));
        Siniestro siniestro = siniestro(EstadoSiniestro.REPORTADO);
        when(siniestros.buscarPorId(siniestro.id())).thenReturn(Optional.of(siniestro));

        assertThatThrownBy(() -> actualizar.execute(otraPoliza, siniestro.id(), EstadoSiniestro.APROBADO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no pertenece");
    }

    private void poliza(EstadoPoliza estado) {
        when(polizas.buscarPorId(polizaId))
                .thenReturn(Optional.of(new PolizaRef(polizaId, clienteId, "POL-2026-1", estado)));
    }

    private Siniestro siniestro(EstadoSiniestro estado) {
        return new Siniestro(
                UUID.randomUUID(),
                polizaId,
                LocalDate.of(2026, 9, 1),
                "CHOQUE",
                Dinero.soles(new BigDecimal("1500")),
                true,
                "LEVE",
                estado);
    }

    private RegistrarSiniestroRequestModel solicitud() {
        return new RegistrarSiniestroRequestModel(
                polizaId, LocalDate.of(2026, 9, 1), "CHOQUE", new BigDecimal("1500"), true, "LEVE", EstadoSiniestro.REPORTADO);
    }

    private DomainEvent capturarEvento() {
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventos).publicar(captor.capture());
        return captor.getValue();
    }
}
