package com.andinaseguros.quotation.frameworksdrivers.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andinaseguros.quotation.entities.enums.EstadoTablaTarifaria;
import com.andinaseguros.quotation.interfaceadapters.out.persistence.mongodb.document.TablaTarifariaDocument;
import com.andinaseguros.quotation.interfaceadapters.out.persistence.mongodb.repository.SpringDataTablaTarifariaMongoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Retiro del monolito: una instalación nueva siembra las tablas tarifarias sin la migración. */
class DemoDataInitializerTest {
    private final SpringDataTablaTarifariaMongoRepository tarifas = mock(SpringDataTablaTarifariaMongoRepository.class);
    private final DemoDataInitializer inicializador = new DemoDataInitializer(tarifas);

    @Test
    void conLaBaseVaciaCreaLasTresTablasDelMonolito() {
        when(tarifas.existsById(any())).thenReturn(false);

        inicializador.run(null);

        ArgumentCaptor<TablaTarifariaDocument> guardadas = ArgumentCaptor.forClass(TablaTarifariaDocument.class);
        verify(tarifas, times(3)).save(guardadas.capture());
        assertThat(guardadas.getAllValues())
                .extracting(t -> t.id)
                .containsExactly(
                        DemoDataInitializer.TARIFA_AUTO, DemoDataInitializer.TARIFA_CAMIONETA, DemoDataInitializer.TARIFA_BORRADOR);
        assertThat(guardadas.getAllValues().get(0).estado).isEqualTo(EstadoTablaTarifaria.VIGENTE);
        assertThat(guardadas.getAllValues().get(0).factores).hasSize(1);
    }

    @Test
    void siYaExistenNoLasToca() {
        when(tarifas.existsById(any())).thenReturn(true);

        inicializador.run(null);

        verify(tarifas, never()).save(any());
    }
}
