package com.andinaseguros.entities.model;

import com.andinaseguros.entities.enums.*;
import com.andinaseguros.entities.valueobject.*;
import java.util.*;

public class TablaTarifaria {
    private final UUID id;
    private final String codigo;
    private final int version;
    private final TipoVehiculo tipoVehiculo;
    private final TipoUso tipoUso;
    private final Dinero primaBase;
    private final Dinero primaMinima;
    private final PeriodoVigencia vigencia;
    private final String codigoNotaTecnica;
    private final EstadoTablaTarifaria estado;
    private final List<FactorRiesgo> factores;

    public TablaTarifaria(
            UUID id,
            String codigo,
            int version,
            TipoVehiculo tipoVehiculo,
            TipoUso tipoUso,
            Dinero primaBase,
            Dinero primaMinima,
            PeriodoVigencia vigencia,
            String codigoNotaTecnica,
            EstadoTablaTarifaria estado,
            List<FactorRiesgo> factores) {
        this.id = id;
        this.codigo = codigo;
        this.version = version;
        this.tipoVehiculo = tipoVehiculo;
        this.tipoUso = tipoUso;
        this.primaBase = primaBase;
        this.primaMinima = primaMinima;
        this.vigencia = vigencia;
        this.codigoNotaTecnica = codigoNotaTecnica;
        this.estado = estado;
        this.factores = List.copyOf(factores == null ? List.of() : factores);
    }

    public boolean vigenteEn(java.time.LocalDate fecha) {
        return estado == EstadoTablaTarifaria.VIGENTE && vigencia.contiene(fecha);
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public int getVersion() {
        return version;
    }

    public TipoVehiculo getTipoVehiculo() {
        return tipoVehiculo;
    }

    public TipoUso getTipoUso() {
        return tipoUso;
    }

    public Dinero getPrimaBase() {
        return primaBase;
    }

    public Dinero getPrimaMinima() {
        return primaMinima;
    }

    public PeriodoVigencia getVigencia() {
        return vigencia;
    }

    public String getCodigoNotaTecnica() {
        return codigoNotaTecnica;
    }

    public EstadoTablaTarifaria getEstado() {
        return estado;
    }

    public List<FactorRiesgo> getFactores() {
        return factores;
    }
}
