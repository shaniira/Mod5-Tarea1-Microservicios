package com.andinaseguros.quotation.usecases.port.out.repository;

import java.util.UUID;

/**
 * Motivos de rechazo de emisión (compensación de la saga, paso 6.4). Se guardan aparte para no
 * cambiar la respuesta de /api/cotizaciones, que debe seguir igual a la del monolito.
 */
public interface RechazoEmisionRepository {
    /** Idempotente por eventId: un evento repetido no duplica el registro. */
    void registrar(UUID eventId, UUID cotizacionId, String codigo, String motivo);
}
