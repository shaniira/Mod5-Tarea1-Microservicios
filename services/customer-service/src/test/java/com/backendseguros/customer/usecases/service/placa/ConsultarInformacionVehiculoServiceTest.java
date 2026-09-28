package com.backendseguros.customer.usecases.service.placa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.backendseguros.customer.usecases.exception.VehicleProviderUnavailableException;
import com.backendseguros.customer.usecases.model.VehicleInformation;
import com.backendseguros.customer.usecases.port.out.vehicle.VehicleInformationCachePort;
import com.backendseguros.customer.usecases.port.out.vehicle.VehicleInformationPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Las cuatro pruebas del monolito más las de la caché de placas (paso 3.2). */
class ConsultarInformacionVehiculoServiceTest {
    private final VehicleInformationPort port = mock(VehicleInformationPort.class);
    private final VehicleInformationCachePort cache = mock(VehicleInformationCachePort.class);
    private final ConsultarInformacionVehiculoService service =
            new ConsultarInformacionVehiculoService(port, cache);
    private final VehicleInformation logan =
            new VehicleInformation("B6U170", "RENAULT", "LOGAN", 2011, null, null, null, "JSON_PE");

    @Test
    void normalizaPlacaYRetornaDatos() {
        when(port.consultarPorPlaca("B6U170")).thenReturn(Optional.of(logan));
        assertThat(service.consultarPorPlaca("b6u170")).isEqualTo(logan);
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
                .thenThrow(new VehicleProviderUnavailableException("Proveedor no disponible", new RuntimeException()));

        var result = service.consultarPorPlaca("B6U170");

        assertThat(result.fuente()).isEqualTo("SIN_DATOS");
        assertThat(result.marca()).isNull();
    }

    @Test
    void unaPlacaEnCacheSeRespondeSinLlamarAlProveedor() {
        when(cache.buscar("B6U170")).thenReturn(Optional.of(logan));

        assertThat(service.consultarPorPlaca("B6U170")).isEqualTo(logan);
        verifyNoInteractions(port);
    }

    @Test
    void guardaEnCacheSoloLoQueElProveedorEncontro() {
        when(port.consultarPorPlaca("B6U170")).thenReturn(Optional.of(logan));
        service.consultarPorPlaca("B6U170");
        verify(cache).guardar(logan);

        when(port.consultarPorPlaca("ZZZ999")).thenReturn(Optional.empty());
        service.consultarPorPlaca("ZZZ999");
        verify(cache, times(1)).guardar(any());
    }
}
