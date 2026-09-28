package com.backendseguros.policy.entities.model;

import java.util.UUID;

/**
 * Copia mínima de un siniestro (proyección claim_ref, paso 6.2), alimentada por claim.*. Guarda
 * exactamente lo que usa la renovación: si está abierto y si el asegurado fue responsable. Con una
 * entrada por siniestro (y su versión) los eventos repetidos o desordenados no descuadran la cuenta,
 * cosa que un contador simple no garantiza.
 */
public record SiniestroRef(UUID siniestroId, UUID polizaId, boolean abierto, boolean responsabilidadAsegurado) {}
