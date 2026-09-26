package com.andinaseguros.entities.model;

import com.andinaseguros.entities.enums.EstadoCotizacion;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.valueobject.Dinero;
import java.time.*;
import java.util.UUID;

public class Cotizacion {
    private final UUID id;
    private final String numero;
    private final UUID clienteId;
    private final UUID vehiculoId;
    private final UUID tablaTarifariaId;
    private final Dinero prima;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaExpiracion;
    private final ResultadoTarificacion desglose;
    private EstadoCotizacion estado;

    public Cotizacion(
            UUID id,
            String numero,
            UUID clienteId,
            UUID vehiculoId,
            UUID tablaTarifariaId,
            Dinero prima,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaExpiracion,
            EstadoCotizacion estado) {
        this(
                id,
                numero,
                clienteId,
                vehiculoId,
                tablaTarifariaId,
                prima,
                fechaCreacion,
                fechaExpiracion,
                estado,
                null);
    }

    public Cotizacion(
            UUID id,
            String numero,
            UUID clienteId,
            UUID vehiculoId,
            UUID tablaTarifariaId,
            Dinero prima,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaExpiracion,
            EstadoCotizacion estado,
            ResultadoTarificacion desglose) {
        this.id = id;
        this.numero = numero;
        this.clienteId = clienteId;
        this.vehiculoId = vehiculoId;
        this.tablaTarifariaId = tablaTarifariaId;
        this.prima = prima;
        this.fechaCreacion = fechaCreacion;
        this.fechaExpiracion = fechaExpiracion;
        this.estado = estado;
        this.desglose = desglose;
    }

    public boolean estaVigente() {
        return estado == EstadoCotizacion.VIGENTE && LocalDateTime.now().isBefore(fechaExpiracion);
    }

    public void aceptar() {
        if (!estaVigente())
            throw new ReglaNegocioException(
                    "COTIZACION_NO_VIGENTE", "La cotización no está vigente");
        estado = EstadoCotizacion.ACEPTADA;
    }

    public void marcarEmitida() {
        if (estado != EstadoCotizacion.ACEPTADA)
            throw new ReglaNegocioException(
                    "COTIZACION_NO_ACEPTADA", "La cotización debe estar aceptada");
        estado = EstadoCotizacion.EMITIDA;
    }

    public UUID getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public UUID getVehiculoId() {
        return vehiculoId;
    }

    public UUID getTablaTarifariaId() {
        return tablaTarifariaId;
    }

    public Dinero getPrima() {
        return prima;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public EstadoCotizacion getEstado() {
        return estado;
    }

    public ResultadoTarificacion getDesglose() {
        return desglose;
    }
}
