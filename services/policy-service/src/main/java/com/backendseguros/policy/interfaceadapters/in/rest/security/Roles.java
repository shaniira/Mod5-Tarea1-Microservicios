package com.backendseguros.policy.interfaceadapters.in.rest.security;

/**
 * Reglas de acceso por rol (riesgo S2), las mismas que tenía el monolito para pólizas, renovaciones
 * y "Mi cuenta" (su Roles.java está en la etiqueta de git monolito-final).
 */
public final class Roles {
    private Roles() {}

    /** Personal interno: puede consultar. */
    public static final String PERSONAL = "hasAnyRole('ADMIN','AGENTE','ACTUARIO')";

    /** Operación comercial: emitir pólizas y decidir renovaciones. */
    public static final String OPERACION = "hasAnyRole('ADMIN','AGENTE')";

    public static final String CLIENTE = "hasRole('CLIENTE')";
}
