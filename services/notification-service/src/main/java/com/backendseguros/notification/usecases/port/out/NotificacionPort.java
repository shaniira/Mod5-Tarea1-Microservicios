package com.backendseguros.notification.usecases.port.out;

import com.backendseguros.notification.entities.model.Notificacion;

public interface NotificacionPort {
    /**
     * @throws com.backendseguros.notification.usecases.exception.CanalNoDisponibleException si el
     *     canal está caído (transitorio: el mensaje debe esperar, no ir a la DLQ)
     * @throws com.backendseguros.notification.usecases.exception.NotificacionRechazadaException si el
     *     proveedor rechaza el mensaje (definitivo: reintentar no cambia nada)
     */
    void enviar(Notificacion notificacion);
}
