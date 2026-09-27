package com.andinaseguros.claims.entities.model;

import com.andinaseguros.claims.entities.enums.EstadoPoliza;
import java.util.UUID;

/**
 * Copia mínima de una póliza (proyección policy_ref, paso 4.3): lo único que claims necesita para
 * registrar un siniestro (que exista y esté VIGENTE) y para el control de propietario (su cliente).
 * La fuente de verdad sigue siendo el dueño de las pólizas; aquí no se modifica nunca por una
 * petición, solo con eventos o con la carga inicial.
 */
public record PolizaRef(UUID polizaId, UUID clienteId, String numero, EstadoPoliza estado) {
    public PolizaRef {
        if (polizaId == null) {
            throw new IllegalArgumentException("La referencia necesita el id de la póliza");
        }
    }

    public boolean estaVigente() {
        return estado == EstadoPoliza.VIGENTE;
    }
}
