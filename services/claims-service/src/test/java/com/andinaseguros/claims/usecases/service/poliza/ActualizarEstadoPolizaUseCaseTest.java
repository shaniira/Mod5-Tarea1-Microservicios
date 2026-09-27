package com.andinaseguros.claims.usecases.service.poliza;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.andinaseguros.claims.entities.enums.EstadoPoliza;
import com.andinaseguros.claims.entities.model.PolizaRef;
import com.andinaseguros.claims.usecases.port.out.repository.PolizaRefRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Fase 6: policy_ref se mantiene con policy.renewed/expired/cancelled.v1 de policy-service. */
class ActualizarEstadoPolizaUseCaseTest {
    private final PolizaRefRepository polizas = mock(PolizaRefRepository.class);
    private final ActualizarEstadoPolizaUseCase useCase = new ActualizarEstadoPolizaUseCase(polizas);

    @Test
    void unaRenovacionRegistraLaNuevaVigenteYMarcaLaAnteriorRenovada() {
        UUID anterior = UUID.randomUUID();
        UUID nueva = UUID.randomUUID();
        UUID cliente = UUID.randomUUID();
        when(polizas.actualizarEstadoSiEsMasNuevo(anterior, EstadoPoliza.RENOVADA, 2)).thenReturn(true);

        var resultado = useCase.renovada(anterior, 2, nueva, cliente, "POL-REN-2027-1");

        assertThat(resultado).isEqualTo(ActualizarEstadoPolizaUseCase.Resultado.APLICADO);
        ArgumentCaptor<PolizaRef> captor = ArgumentCaptor.forClass(PolizaRef.class);
        verify(polizas).registrarEmitidaSiNoExiste(captor.capture());
        assertThat(captor.getValue().polizaId()).isEqualTo(nueva);
        assertThat(captor.getValue().clienteId()).isEqualTo(cliente);
        assertThat(captor.getValue().estado()).isEqualTo(EstadoPoliza.VIGENTE);
    }

    @Test
    void unEventoViejoSeIgnora() {
        UUID poliza = UUID.randomUUID();
        when(polizas.actualizarEstadoSiEsMasNuevo(any(), any(), anyLong())).thenReturn(false);

        assertThat(useCase.cambiarEstado(poliza, EstadoPoliza.VENCIDA, 1))
                .isEqualTo(ActualizarEstadoPolizaUseCase.Resultado.IGNORADO);
    }
}
