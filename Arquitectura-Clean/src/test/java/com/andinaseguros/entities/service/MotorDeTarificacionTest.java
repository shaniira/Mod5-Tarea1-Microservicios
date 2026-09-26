package com.andinaseguros.entities.service;

import static org.junit.jupiter.api.Assertions.*;

import com.andinaseguros.entities.enums.*;
import com.andinaseguros.entities.model.*;
import com.andinaseguros.entities.valueobject.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class MotorDeTarificacionTest {
    @Test
    void calculaPrimaConFactoresOrdenados() {
        Cliente c =
                new Cliente(
                        UUID.randomUUID(),
                        "DNI",
                        "12345678",
                        "Ana",
                        "Pérez",
                        LocalDate.of(1995, 1, 1),
                        "a@b.com",
                        "999",
                        true);
        Vehiculo v =
                new Vehiculo(
                        UUID.randomUUID(),
                        c.getId(),
                        new Placa("ABC-123"),
                        "Toyota",
                        "Yaris",
                        2020,
                        TipoVehiculo.AUTO,
                        TipoUso.PARTICULAR,
                        "LIMA");
        TablaTarifaria t =
                new TablaTarifaria(
                        UUID.randomUUID(),
                        "AUTO-PART",
                        1,
                        TipoVehiculo.AUTO,
                        TipoUso.PARTICULAR,
                        Dinero.soles(new BigDecimal("1000")),
                        Dinero.soles(new BigDecimal("700")),
                        new PeriodoVigencia(
                                LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)),
                        "NT-001",
                        EstadoTablaTarifaria.VIGENTE,
                        List.of(
                                new FactorRiesgo(
                                        UUID.randomUUID(),
                                        "EDAD",
                                        "Edad",
                                        "EDAD_CONDUCTOR",
                                        new BigDecimal("18"),
                                        new BigDecimal("35"),
                                        new BigDecimal("1.20"),
                                        1),
                                new FactorRiesgo(
                                        UUID.randomUUID(),
                                        "ANT",
                                        "Antigüedad",
                                        "ANTIGUEDAD_VEHICULO",
                                        BigDecimal.ZERO,
                                        new BigDecimal("10"),
                                        new BigDecimal("1.10"),
                                        2)));
        ResultadoTarificacion r =
                new MotorDeTarificacion()
                        .calcular(
                                t,
                                c,
                                v,
                                0,
                                new BigDecimal("0.10"),
                                new BigDecimal("0.03"),
                                new BigDecimal("0.05"));
        assertEquals(new BigDecimal("1417.02"), r.primaComercial().valor());
        assertEquals(2, r.factores().size());
    }
}
