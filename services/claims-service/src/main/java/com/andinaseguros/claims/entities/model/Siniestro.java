package com.andinaseguros.claims.entities.model;

import com.andinaseguros.claims.entities.enums.EstadoSiniestro;
import com.andinaseguros.claims.entities.exception.ReglaNegocioException;
import com.andinaseguros.claims.entities.valueobject.Dinero;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Misma regla que en el monolito (un siniestro cerrado no cambia). Se agrega la versión del
 * agregado: sube en cada cambio de estado y viaja en claim.*, así los consumidores descartan los
 * eventos viejos o repetidos.
 */
public class Siniestro {
    private final UUID id;
    private final UUID polizaId;
    private final LocalDate fecha;
    private final String tipo;
    private final Dinero montoEstimado;
    private final boolean responsabilidadAsegurado;
    private final String gravedad;
    private EstadoSiniestro estado;
    private long version;

    public Siniestro(
            UUID id,
            UUID polizaId,
            LocalDate fecha,
            String tipo,
            Dinero montoEstimado,
            boolean responsabilidadAsegurado,
            String gravedad,
            EstadoSiniestro estado) {
        this(id, polizaId, fecha, tipo, montoEstimado, responsabilidadAsegurado, gravedad, estado, 1);
    }

    public Siniestro(
            UUID id,
            UUID polizaId,
            LocalDate fecha,
            String tipo,
            Dinero montoEstimado,
            boolean responsabilidadAsegurado,
            String gravedad,
            EstadoSiniestro estado,
            long version) {
        this.id = id;
        this.polizaId = polizaId;
        this.fecha = fecha;
        this.tipo = tipo;
        this.montoEstimado = montoEstimado;
        this.responsabilidadAsegurado = responsabilidadAsegurado;
        this.gravedad = gravedad;
        this.estado = estado;
        this.version = version;
    }

    public void cambiarEstado(EstadoSiniestro nuevoEstado) {
        if (nuevoEstado == null) throw new IllegalArgumentException("El estado es obligatorio");
        if (!estaAbierto())
            throw new ReglaNegocioException(
                    "SINIESTRO_CERRADO", "Un siniestro cerrado no puede modificarse");
        estado = nuevoEstado;
        version++;
    }

    /** Abierto = aún no LIQUIDADO ni RECHAZADO: bloquea la renovación de la póliza. */
    public boolean estaAbierto() {
        return estado != EstadoSiniestro.LIQUIDADO && estado != EstadoSiniestro.RECHAZADO;
    }

    public UUID id() {
        return id;
    }

    public UUID polizaId() {
        return polizaId;
    }

    public LocalDate fecha() {
        return fecha;
    }

    public String tipo() {
        return tipo;
    }

    public Dinero montoEstimado() {
        return montoEstimado;
    }

    public boolean responsabilidadAsegurado() {
        return responsabilidadAsegurado;
    }

    public String gravedad() {
        return gravedad;
    }

    public EstadoSiniestro estado() {
        return estado;
    }

    public long version() {
        return version;
    }
}
