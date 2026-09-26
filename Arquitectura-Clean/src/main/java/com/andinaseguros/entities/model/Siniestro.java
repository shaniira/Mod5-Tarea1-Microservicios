package com.andinaseguros.entities.model;

import com.andinaseguros.entities.enums.EstadoSiniestro;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.valueobject.Dinero;
import java.time.LocalDate;
import java.util.UUID;

public class Siniestro {
    private final UUID id;
    private final UUID polizaId;
    private final LocalDate fecha;
    private final String tipo;
    private final Dinero montoEstimado;
    private final boolean responsabilidadAsegurado;
    private final String gravedad;
    private EstadoSiniestro estado;

    public Siniestro(
            UUID id,
            UUID polizaId,
            LocalDate fecha,
            String tipo,
            Dinero montoEstimado,
            boolean responsabilidadAsegurado,
            String gravedad,
            EstadoSiniestro estado) {
        this.id = id;
        this.polizaId = polizaId;
        this.fecha = fecha;
        this.tipo = tipo;
        this.montoEstimado = montoEstimado;
        this.responsabilidadAsegurado = responsabilidadAsegurado;
        this.gravedad = gravedad;
        this.estado = estado;
    }

    public void cambiarEstado(EstadoSiniestro nuevoEstado) {
        if (nuevoEstado == null) throw new IllegalArgumentException("El estado es obligatorio");
        if (estado == EstadoSiniestro.LIQUIDADO || estado == EstadoSiniestro.RECHAZADO)
            throw new ReglaNegocioException(
                    "SINIESTRO_CERRADO", "Un siniestro cerrado no puede modificarse");
        estado = nuevoEstado;
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
}
