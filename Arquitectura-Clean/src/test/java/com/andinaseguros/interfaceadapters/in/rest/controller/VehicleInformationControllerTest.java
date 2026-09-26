package com.andinaseguros.interfaceadapters.in.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.andinaseguros.usecases.model.VehicleInformation;
import com.andinaseguros.usecases.port.in.ConsultarInformacionVehiculoUseCase;
import org.junit.jupiter.api.Test;

class VehicleInformationControllerTest {
    @Test
    void retornaHttp200SinAccederAlProveedor() {
        var useCase = mock(ConsultarInformacionVehiculoUseCase.class);
        when(useCase.consultarPorPlaca("B6U170"))
                .thenReturn(
                        new VehicleInformation(
                                "B6U170", "RENAULT", "LOGAN", 2011, null, null, null, "FAKE"));
        var response = new VehicleInformationController(useCase).consultar("B6U170");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().fuente()).isEqualTo("FAKE");
    }
}
