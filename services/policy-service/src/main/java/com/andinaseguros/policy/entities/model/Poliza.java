package com.andinaseguros.policy.entities.model;

import com.andinaseguros.policy.entities.enums.EstadoPoliza;
import com.andinaseguros.policy.entities.valueobject.Dinero;
import com.andinaseguros.policy.entities.valueobject.PeriodoVigencia;
import java.util.UUID;

/**
 * Misma póliza que el monolito, más la versión del agregado: 1 al emitirse, sube en cada cambio de
 * estado y viaja en policy.* para que los consumidores (claims) descarten eventos viejos.
 */
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
    private long version;

    public Poliza(
            UUID id,
            String numero,
            UUID cotizacionId,
            UUID clienteId,
            UUID vehiculoId,
            Dinero prima,
            PeriodoVigencia vigencia,
            EstadoPoliza estado,
            UUID renovacionOrigenId,
            long version) {
        this.id = id;
        this.numero = numero;
        this.cotizacionId = cotizacionId;
        this.clienteId = clienteId;
        this.vehiculoId = vehiculoId;
        this.prima = prima;
        this.vigencia = vigencia;
        this.estado = estado;
        this.renovacionOrigenId = renovacionOrigenId;
        this.version = version;
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

    public long getVersion() {
        return version;
    }

    public void marcarRenovada() {
        estado = EstadoPoliza.RENOVADA;
        version++;
    }
}
