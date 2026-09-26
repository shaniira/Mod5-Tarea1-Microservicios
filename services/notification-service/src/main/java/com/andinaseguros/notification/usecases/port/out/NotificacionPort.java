package com.andinaseguros.notification.usecases.port.out;

import com.andinaseguros.notification.entities.model.Notificacion;

public interface NotificacionPort {
    /**
     * @throws com.andinaseguros.notification.usecases.exception.CanalNoDisponibleException si el
     *     canal está caído (transitorio: el mensaje debe esperar, no ir a la DLQ)
     * @throws com.andinaseguros.notification.usecases.exception.NotificacionRechazadaException si el
     *     proveedor rechaza el mensaje (definitivo: reintentar no cambia nada)
     */
    void enviar(Notificacion notificacion);
}
