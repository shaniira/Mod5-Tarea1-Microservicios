package com.andinaseguros.notification.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhatsAppNotificationAdapter implements NotificationPort {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificationAdapter.class);
    private final WhatsAppClient client;

    public WhatsAppNotificationAdapter(WhatsAppClient client) {
        this.client = client;
    }

    @Override
    public void enviar(NotificationMessage message) {
        String number = normalizarNumero(message.destinatario());
        WhatsAppTextResponse response = client.enviarTexto(new WhatsAppTextRequest(number, message.contenido()));
        if (response == null || !response.success()) {
            throw new IllegalStateException(
                    "JSON.pe rechazo el mensaje: " + (response == null ? "respuesta vacia" : response.message()));
        }
        log.info("WhatsApp enviado correctamente a {}", enmascarar(number));
    }

    private String normalizarNumero(String phone) {
        String number = phone == null ? "" : phone.replaceAll("[^0-9]", "");
        if (number.isBlank()) {
            throw new IllegalArgumentException("El telefono de destino es obligatorio");
        }
        return number;
    }

    private String enmascarar(String number) {
        int visibleDigits = Math.min(4, number.length());
        return "***" + number.substring(number.length() - visibleDigits);
    }
}
