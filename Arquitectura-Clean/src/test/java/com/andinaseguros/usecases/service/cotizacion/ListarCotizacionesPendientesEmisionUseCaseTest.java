package com.andinaseguros.usecases.service.cotizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.andinaseguros.entities.enums.EstadoCotizacion;
import com.andinaseguros.entities.model.Cotizacion;
import com.andinaseguros.entities.model.Poliza;
import com.andinaseguros.usecases.port.out.repository.CotizacionRepository;
import com.andinaseguros.usecases.port.out.repository.PolizaRepository;
import com.andinaseguros.entities.valueobject.Dinero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListarCotizacionesPendientesEmisionUseCaseTest {
    @Test
    void excluyeCotizacionesQueYaTienenPoliza() {
        var cotizaciones = mock(CotizacionRepository.class);
        var polizas = mock(PolizaRepository.class);
        var pendiente = cotizacion();
        var emitida = cotizacion();
        when(cotizaciones.listarPorEstado(EstadoCotizacion.ACEPTADA))
                .thenReturn(List.of(pendiente, emitida));
        when(polizas.buscarPorCotizacionId(pendiente.getId())).thenReturn(Optional.empty());
        when(polizas.buscarPorCotizacionId(emitida.getId()))
                .thenReturn(Optional.of(mock(Poliza.class)));

        var resultado =
                new ListarCotizacionesPendientesEmisionUseCase(cotizaciones, polizas).execute();

        assertThat(resultado).extracting(item -> item.id()).containsExactly(pendiente.getId());
    }

    private Cotizacion cotizacion() {
        return new Cotizacion(
                UUID.randomUUID(),
                "COT-TEST",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Dinero.soles(new BigDecimal("1200")),
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(10),
                EstadoCotizacion.ACEPTADA);
    }
}
