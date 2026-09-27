package com.andinaseguros.policy.usecases.port.out.repository;

/**
 * ¿La proyección claim_ref está al día? (paso 6.5: "si el contador no está al día, tratarlo como
 * hay pendientes"). Debe responder false ante la duda: una renovación con un siniestro abierto que
 * todavía no llegó sería un error de negocio; pedir que se reintente, no.
 */
public interface SincronizacionSiniestrosPort {
    boolean estaAlDia();
}
