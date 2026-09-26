package com.andinaseguros.usecases.service.poliza;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.andinaseguros.usecases.dto.EmitirPolizaRequestModel;
import com.andinaseguros.entities.enums.EstadoCotizacion;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.model.*;
import com.andinaseguros.usecases.port.out.repository.*;
import com.andinaseguros.entities.valueobject.Dinero;
import com.andinaseguros.usecases.port.out.event.DomainEventPublisherPort;
import com.andinaseguros.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.usecases.port.out.time.ClockPort;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class EmitirPolizaUseCaseTest {
    private final CotizacionRepository cotizaciones = mock(CotizacionRepository.class);
    private final PolizaRepository polizas = mock(PolizaRepository.class);
    private final DomainEventPublisherPort eventos = mock(DomainEventPublisherPort.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
    private final UUID cotizacionId = UUID.randomUUID();
    private final UUID clienteId = UUID.randomUUID();
    private final Cotizacion cotizacion =
            new Cotizacion(
                    cotizacionId,
                    "COT-1",
                    clienteId,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    new Dinero(new BigDecimal("1000"), "PEN"),
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1),
                    EstadoCotizacion.ACEPTADA);

    @Test
    void emiteYPublicaEvento() {
        when(cotizaciones.buscarPorId(cotizacionId)).thenReturn(Optional.of(cotizacion));
        when(polizas.buscarPorCotizacionId(cotizacionId)).thenReturn(Optional.empty());
        when(polizas.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(clock.now()).thenReturn(Instant.parse("2026-07-27T12:00:00Z"));
        when(ids.generar()).thenReturn(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        var useCase = new EmitirPolizaUseCase(cotizaciones, polizas, eventos, clock, ids);

        var respuesta = useCase.execute(new EmitirPolizaRequestModel(cotizacionId, LocalDate.now()));

        assertThat(respuesta.cotizacionId()).isEqualTo(cotizacionId);
        verify(eventos).publicar(any());
    }

    @Test
    void impideDobleEmisionYNoPublicaEvento() {
        when(cotizaciones.buscarPorId(cotizacionId)).thenReturn(Optional.of(cotizacion));
        when(polizas.buscarPorCotizacionId(cotizacionId))
                .thenReturn(Optional.of(mock(Poliza.class)));
        var useCase = new EmitirPolizaUseCase(cotizaciones, polizas, eventos, clock, ids);

        assertThatThrownBy(() -> useCase.execute(new EmitirPolizaRequestModel(cotizacionId, LocalDate.now())))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(eventos);
    }
}
