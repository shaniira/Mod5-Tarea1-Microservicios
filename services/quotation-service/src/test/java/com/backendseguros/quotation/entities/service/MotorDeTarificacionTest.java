package com.backendseguros.quotation.entities.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.backendseguros.quotation.entities.enums.EstadoTablaTarifaria;
import com.backendseguros.quotation.entities.enums.TipoUso;
import com.backendseguros.quotation.entities.enums.TipoVehiculo;
import com.backendseguros.quotation.entities.exception.ReglaNegocioException;
import com.backendseguros.quotation.entities.model.ClienteRef;
import com.backendseguros.quotation.entities.model.FactorRiesgo;
import com.backendseguros.quotation.entities.model.ResultadoTarificacion;
import com.backendseguros.quotation.entities.model.TablaTarifaria;
import com.backendseguros.quotation.entities.model.VehiculoRef;
import com.backendseguros.quotation.entities.valueobject.Dinero;
import com.backendseguros.quotation.entities.valueobject.PeriodoVigencia;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Paso 5.7: el caso del monolito (mismas entradas, mismo resultado: 1417.02) y casos de referencia
 * adicionales que cubren la prima mínima y los siniestros.
 */
class MotorDeTarificacionTest {
    private final MotorDeTarificacion motor = new MotorDeTarificacion();
    private final ClienteRef ana = new ClienteRef(UUID.randomUUID(), LocalDate.of(1995, 1, 1), true);
    private final VehiculoRef yaris =
            new VehiculoRef(UUID.randomUUID(), ana.clienteId(), TipoVehiculo.AUTO, TipoUso.PARTICULAR, 2020);

    @Test
    void calculaPrimaConFactoresOrdenados() {
        ResultadoTarificacion r =
                motor.calcular(
                        tabla("1000", "700", List.of(edad(), antiguedad())),
                        ana,
                        yaris,
                        0,
                        new BigDecimal("0.10"),
                        new BigDecimal("0.03"),
                        new BigDecimal("0.05"));
        assertEquals(new BigDecimal("1417.02"), r.primaComercial().valor());
        assertEquals(2, r.factores().size());
    }

    @Test
    void conMasDeDosSiniestrosLaPrimaDeRiesgoNoBajaDeLaMinima() {
        ResultadoTarificacion r =
                motor.calcular(
                        tabla("500", "700", List.of()),
                        ana,
                        yaris,
                        3,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO);
        assertEquals(new BigDecimal("700.00"), r.primaRiesgo().valor());
        assertEquals(new BigDecimal("700.00"), r.primaComercial().valor());
    }

    @Test
    void laPrimaComercialNuncaBajaDeLaMinima() {
        ResultadoTarificacion r =
                motor.calcular(
                        tabla("600", "700", List.of()),
                        ana,
                        yaris,
                        0,
                        new BigDecimal("0.10"),
                        new BigDecimal("0.03"),
                        new BigDecimal("0.50"));
        assertEquals(new BigDecimal("700.00"), r.primaComercial().valor());
    }

    @Test
    void rechazaUnaTablaDeOtroTipoDeVehiculo() {
        VehiculoRef moto =
                new VehiculoRef(UUID.randomUUID(), ana.clienteId(), TipoVehiculo.MOTO, TipoUso.PARTICULAR, 2020);
        assertThrows(
                ReglaNegocioException.class,
                () -> motor.calcular(tabla("1000", "700", List.of()), ana, moto, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private static TablaTarifaria tabla(String base, String minima, List<FactorRiesgo> factores) {
        return new TablaTarifaria(
                UUID.randomUUID(),
                "AUTO-PART",
                1,
                TipoVehiculo.AUTO,
                TipoUso.PARTICULAR,
                Dinero.soles(new BigDecimal(base)),
                Dinero.soles(new BigDecimal(minima)),
                new PeriodoVigencia(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)),
                "NT-001",
                EstadoTablaTarifaria.VIGENTE,
                factores);
    }

    private static FactorRiesgo edad() {
        return new FactorRiesgo(
                UUID.randomUUID(), "EDAD", "Edad", "EDAD_CONDUCTOR", new BigDecimal("18"), new BigDecimal("35"), new BigDecimal("1.20"), 1);
    }

    private static FactorRiesgo antiguedad() {
        return new FactorRiesgo(
                UUID.randomUUID(), "ANT", "Antigüedad", "ANTIGUEDAD_VEHICULO", BigDecimal.ZERO, new BigDecimal("10"), new BigDecimal("1.10"), 2);
    }
}
