package com.andinaseguros.entities.model;

import com.andinaseguros.entities.enums.EstadoPoliza;
import com.andinaseguros.entities.valueobject.*;
import java.util.UUID;

public class Poliza {
    private final UUID id;
    private final String numero;
    private final UUID cotizacionId;
    private final UUID clienteId;
    private final UUID vehiculoId;
    private final Dinero prima;
    private final PeriodoVigencia vigencia;
    private final UUID renovacionOrigenId;
    private EstadoPoliza estado;

    public Poliza(
            UUID id,
            String numero,
            UUID cotizacionId,
            UUID clienteId,
            UUID vehiculoId,
            Dinero prima,
            PeriodoVigencia vigencia,
            EstadoPoliza estado) {
        this(id, numero, cotizacionId, clienteId, vehiculoId, prima, vigencia, estado, null);
    }

    public Poliza(
            UUID id,
            String numero,
            UUID cotizacionId,
            UUID clienteId,
            UUID vehiculoId,
            Dinero prima,
            PeriodoVigencia vigencia,
            EstadoPoliza estado,
            UUID renovacionOrigenId) {
        this.id = id;
        this.numero = numero;
        this.cotizacionId = cotizacionId;
        this.clienteId = clienteId;
        this.vehiculoId = vehiculoId;
        this.prima = prima;
        this.vigencia = vigencia;
        this.estado = estado;
        this.renovacionOrigenId = renovacionOrigenId;
    }

    public UUID getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public UUID getCotizacionId() {
        return cotizacionId;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public UUID getVehiculoId() {
        return vehiculoId;
    }

    public Dinero getPrima() {
        return prima;
    }

    public PeriodoVigencia getVigencia() {
        return vigencia;
    }

    public EstadoPoliza getEstado() {
        return estado;
    }

    public UUID getRenovacionOrigenId() {
        return renovacionOrigenId;
    }

    public void activar() {
        estado = EstadoPoliza.VIGENTE;
    }

    public void marcarRenovada() {
        estado = EstadoPoliza.RENOVADA;
    }
}
