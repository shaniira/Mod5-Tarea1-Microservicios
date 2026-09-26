package com.andinaseguros.entities.service;

import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.model.*;
import com.andinaseguros.entities.valueobject.Dinero;
import java.math.*;
import java.time.LocalDate;
import java.util.*;

public class MotorDeTarificacion {
    public ResultadoTarificacion calcular(
            TablaTarifaria tabla,
            Cliente cliente,
            Vehiculo vehiculo,
            int siniestrosResponsables,
            BigDecimal porcentajeGastos,
            BigDecimal porcentajeRecargo,
            BigDecimal porcentajeDescuento) {
        if (!tabla.vigenteEn(LocalDate.now()))
            throw new ReglaNegocioException(
                    "TABLA_NO_VIGENTE", "La tabla tarifaria no está vigente");
        if (tabla.getTipoVehiculo() != vehiculo.getTipo()
                || tabla.getTipoUso() != vehiculo.getUso())
            throw new ReglaNegocioException("TABLA_NO_APLICA", "La tabla no aplica al vehículo");
        Map<String, BigDecimal> variables =
                Map.of(
                        "EDAD_CONDUCTOR", BigDecimal.valueOf(cliente.edad()),
                        "ANTIGUEDAD_VEHICULO", BigDecimal.valueOf(vehiculo.antiguedad()),
                        "SINIESTROS", BigDecimal.valueOf(siniestrosResponsables));
        BigDecimal multiplicador = BigDecimal.ONE;
        List<ResultadoTarificacion.FactorAplicado> aplicados = new ArrayList<>();
        for (FactorRiesgo factor :
                tabla.getFactores().stream()
                        .sorted(Comparator.comparingInt(FactorRiesgo::orden))
                        .toList()) {
            BigDecimal valor = variables.get(factor.tipoVariable());
            if (valor != null && factor.aplica(valor)) {
                multiplicador = multiplicador.multiply(factor.multiplicador());
                aplicados.add(
                        new ResultadoTarificacion.FactorAplicado(
                                factor.codigo(), factor.nombre(), valor, factor.multiplicador()));
            }
        }
        Dinero primaRiesgo = tabla.getPrimaBase().multiplicar(multiplicador);
        if (siniestrosResponsables > 2
                && primaRiesgo.valor().compareTo(tabla.getPrimaMinima().valor()) < 0)
            primaRiesgo = tabla.getPrimaMinima();
        Dinero gastos = primaRiesgo.multiplicar(porcentajeGastos);
        Dinero recargos = primaRiesgo.multiplicar(porcentajeRecargo);
        Dinero subtotal = primaRiesgo.sumar(gastos).sumar(recargos);
        Dinero descuentos = subtotal.multiplicar(porcentajeDescuento);
        Dinero comercial = subtotal.restar(descuentos);
        if (comercial.valor().compareTo(tabla.getPrimaMinima().valor()) < 0)
            comercial = tabla.getPrimaMinima();
        return new ResultadoTarificacion(
                tabla.getPrimaBase(),
                primaRiesgo,
                gastos,
                recargos,
                descuentos,
                comercial,
                aplicados);
    }
}
