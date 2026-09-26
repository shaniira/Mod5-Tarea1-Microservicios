package com.andinaseguros.entities.model;

import com.andinaseguros.entities.enums.EstadoRenovacion;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.valueobject.Dinero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PropuestaRenovacion {
    private final UUID id;
    private final UUID polizaOrigenId;
    private final Dinero primaAnterior;
    private final Dinero nuevaPrima;
    private final BigDecimal porcentajeVariacion;
    private final int siniestrosConsiderados;
    private EstadoRenovacion estado;
    private final String motivo;
    private final LocalDateTime creadaEn;
    private final LocalDateTime venceEn;
    private LocalDateTime decididaEn;
    private UUID polizaRenovadaId;

    public PropuestaRenovacion(
            UUID id,
            UUID polizaOrigenId,
            Dinero primaAnterior,
            Dinero nuevaPrima,
            BigDecimal porcentajeVariacion,
            int siniestrosConsiderados,
            EstadoRenovacion estado,
            String motivo,
            LocalDateTime creadaEn,
            LocalDateTime venceEn,
            LocalDateTime decididaEn,
            UUID polizaRenovadaId) {
        this.id = id;
        this.polizaOrigenId = polizaOrigenId;
        this.primaAnterior = primaAnterior;
        this.nuevaPrima = nuevaPrima;
        this.porcentajeVariacion = porcentajeVariacion;
        this.siniestrosConsiderados = siniestrosConsiderados;
        this.estado = estado;
        this.motivo = motivo;
        this.creadaEn = creadaEn;
        this.venceEn = venceEn;
        this.decididaEn = decididaEn;
        this.polizaRenovadaId = polizaRenovadaId;
    }

    public void aprobar(LocalDateTime ahora) {
        validarPendiente(ahora);
        estado = EstadoRenovacion.ACEPTADA;
        decididaEn = ahora;
    }

    public void rechazar(LocalDateTime ahora) {
        validarPendiente(ahora);
        estado = EstadoRenovacion.RECHAZADA;
        decididaEn = ahora;
    }

    public void vincularPolizaRenovada(UUID polizaId) {
        if (estado != EstadoRenovacion.ACEPTADA)
            throw new ReglaNegocioException(
                    "RENOVACION_NO_APROBADA", "La propuesta debe estar aprobada");
        if (polizaRenovadaId != null)
            throw new ReglaNegocioException(
                    "POLIZA_RENOVADA_EXISTENTE", "La propuesta ya generó una póliza");
        polizaRenovadaId = polizaId;
    }

    private void validarPendiente(LocalDateTime ahora) {
        if (estado == EstadoRenovacion.ACEPTADA
                || estado == EstadoRenovacion.RECHAZADA
                || estado == EstadoRenovacion.VENCIDA)
            throw new ReglaNegocioException(
                    "RENOVACION_YA_DECIDIDA", "La propuesta ya fue decidida");
        if (ahora.isAfter(venceEn)) {
            estado = EstadoRenovacion.VENCIDA;
            throw new ReglaNegocioException(
                    "RENOVACION_VENCIDA", "La propuesta de renovación está vencida");
        }
    }

    public UUID id() {
        return id;
    }

    public UUID polizaOrigenId() {
        return polizaOrigenId;
    }

    public Dinero primaAnterior() {
        return primaAnterior;
    }

    public Dinero nuevaPrima() {
        return nuevaPrima;
    }

    public BigDecimal porcentajeVariacion() {
        return porcentajeVariacion;
    }

    public int siniestrosConsiderados() {
        return siniestrosConsiderados;
    }

    public EstadoRenovacion estado() {
        return estado;
    }

    public String motivo() {
        return motivo;
    }

    public LocalDateTime creadaEn() {
        return creadaEn;
    }

    public LocalDateTime venceEn() {
        return venceEn;
    }

    public LocalDateTime decididaEn() {
        return decididaEn;
    }

    public UUID polizaRenovadaId() {
        return polizaRenovadaId;
    }
}
