package com.backendseguros.quotation.usecases.service.cotizacion;

import com.backendseguros.quotation.entities.enums.EstadoCotizacion;
import com.backendseguros.quotation.entities.model.Cotizacion;
import com.backendseguros.quotation.usecases.port.out.repository.CotizacionRepository;
import java.util.UUID;

/**
 * Consume policy.issued.v1 (paso 5.4): la cotización de la póliza pasa a EMITIDA. Es idempotente:
 * si ya estaba EMITIDA no hace nada, así que un evento repetido no causa daño y no hace falta una
 * colección inbox.
 */
public class MarcarCotizacionEmitidaUseCase {
    public enum Resultado {
        MARCADA,
        YA_EMITIDA,
        NO_EXISTE,
        ESTADO_INESPERADO
    }

    private final CotizacionRepository cotizaciones;

    public MarcarCotizacionEmitidaUseCase(CotizacionRepository cotizaciones) {
        this.cotizaciones = cotizaciones;
    }

    public Resultado execute(UUID cotizacionId) {
        var encontrada = cotizaciones.buscarPorId(cotizacionId);
        if (encontrada.isEmpty()) {
            // Cotización creada en el monolito mientras no hay corte: no es de este servicio todavía.
            return Resultado.NO_EXISTE;
        }
        Cotizacion cotizacion = encontrada.get();
        if (cotizacion.getEstado() == EstadoCotizacion.EMITIDA) {
            return Resultado.YA_EMITIDA;
        }
        if (cotizacion.getEstado() != EstadoCotizacion.ACEPTADA) {
            return Resultado.ESTADO_INESPERADO;
        }
        cotizacion.marcarEmitida();
        cotizaciones.guardar(cotizacion);
        return Resultado.MARCADA;
    }
}
