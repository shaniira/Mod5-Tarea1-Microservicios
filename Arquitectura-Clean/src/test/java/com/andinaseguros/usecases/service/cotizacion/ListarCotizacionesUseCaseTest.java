package com.andinaseguros.usecases.service.cotizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.andinaseguros.entities.enums.EstadoCotizacion;
import com.andinaseguros.entities.model.Cotizacion;
import com.andinaseguros.usecases.port.out.repository.CotizacionRepository;
import com.andinaseguros.entities.valueobject.Dinero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListarCotizacionesUseCaseTest {
    private final CotizacionRepository repository = mock(CotizacionRepository.class);
    private final ListarCotizacionesUseCase useCase = new ListarCotizacionesUseCase(repository);

    @Test
    void listaTodasLasCotizaciones() {
        when(repository.listar()).thenReturn(List.of(cotizacion(EstadoCotizacion.VIGENTE)));

        var resultado = useCase.execute(null);

        assertThat(resultado).hasSize(1);
        verify(repository).listar();
    }

    @Test
    void filtraCotizacionesAceptadas() {
        when(repository.listarPorEstado(EstadoCotizacion.ACEPTADA))
                .thenReturn(List.of(cotizacion(EstadoCotizacion.ACEPTADA)));

        var resultado = useCase.execute(EstadoCotizacion.ACEPTADA);

        assertThat(resultado)
                .extracting(item -> item.estado())
                .containsExactly(EstadoCotizacion.ACEPTADA);
    }

    @Test
    void devuelveListaVacia() {
        when(repository.listar()).thenReturn(List.of());

        assertThat(useCase.execute(null)).isEmpty();
    }

    private Cotizacion cotizacion(EstadoCotizacion estado) {
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
