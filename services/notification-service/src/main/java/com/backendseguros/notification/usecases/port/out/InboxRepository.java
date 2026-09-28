package com.backendseguros.notification.usecases.port.out;

import java.util.UUID;

/** Inbox (sección 5.3 de la propuesta): eventos ya procesados por este servicio. */
public interface InboxRepository {
    boolean fueProcesado(UUID eventId);

    void registrar(UUID eventId, String tipoEvento);
}
