package com.andinaseguros.notification.entities.model;

import java.util.Map;

public record Notificacion(String destinatario, String contenido, Map<String, String> parametros) {
    public Notificacion {
        parametros = parametros == null ? Map.of() : Map.copyOf(parametros);
    }

    public static Notificacion polizaEmitida(ContactoCliente contacto, String numeroPoliza) {
        return new Notificacion(
                contacto.telefono(),
                "Su poliza " + numeroPoliza + " fue emitida correctamente.",
                Map.of(
                        "customerId", contacto.clienteId().toString(),
                        "policyNumber", numeroPoliza,
                        "customerName", contacto.nombre() == null ? "" : contacto.nombre()));
    }
}
