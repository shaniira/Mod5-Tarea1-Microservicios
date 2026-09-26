package com.andinaseguros.usecases.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.andinaseguros.usecases.exception.VehicleProviderUnavailableException;
import com.andinaseguros.usecases.model.VehicleInformation;
import com.andinaseguros.usecases.port.out.vehicle.VehicleInformationPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConsultarInformacionVehiculoServiceTest {
    private final VehicleInformationPort port = mock(VehicleInformationPort.class);
    private final ConsultarInformacionVehiculoService service =
            new ConsultarInformacionVehiculoService(port);

    @Test
    void normalizaPlacaYRetornaDatos() {
        var data =
                new VehicleInformation(
                        "B6U170", "RENAULT", "LOGAN", 2011, null, null, null, "PLACA_API");
        when(port.consultarPorPlaca("B6U170")).thenReturn(Optional.of(data));
        assertThat(service.consultarPorPlaca("b6u170")).isEqualTo(data);
    }

    @Test
    void retornaSinDatosCuandoPuertoEstaVacio() {
        when(port.consultarPorPlaca("B6U170")).thenReturn(Optional.empty());
        var result = service.consultarPorPlaca("B6U170");
        assertThat(result.fuente()).isEqualTo("SIN_DATOS");
        assertThat(result.marca()).isNull();
    }

    @Test
    void eliminaEspaciosYGuionesAntesDeConsultar() {
        when(port.consultarPorPlaca("B6U170")).thenReturn(Optional.empty());

        var result = service.consultarPorPlaca(" b6u-170 ");

        verify(port).consultarPorPlaca("B6U170");
        assertThat(result.placa()).isEqualTo("B6U170");
    }

    @Test
    void mantieneIngresoManualCuandoProveedorNoEstaDisponible() {
        when(port.consultarPorPlaca("B6U170"))
                .thenThrow(
                        new VehicleProviderUnavailableException(
                                "Proveedor no disponible", new RuntimeException()));

        var result = service.consultarPorPlaca("B6U170");

        assertThat(result.fuente()).isEqualTo("SIN_DATOS");
        assertThat(result.marca()).isNull();
    }
}
