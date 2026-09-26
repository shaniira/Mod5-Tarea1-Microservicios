package com.andinaseguros.notification.notification;

import java.util.Map;

public record NotificationMessage(
        String destinatario, String contenido, Map<String, String> parametros) {
    public NotificationMessage {
        parametros = parametros == null ? Map.of() : Map.copyOf(parametros);
    }
}
