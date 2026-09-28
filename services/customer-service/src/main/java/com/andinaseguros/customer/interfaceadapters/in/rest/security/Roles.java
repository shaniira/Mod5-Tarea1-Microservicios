package com.andinaseguros.customer.interfaceadapters.in.rest.security;

/**
 * Reglas de acceso por rol (riesgo S2), las mismas que tenía el monolito para clientes y vehículos
 * (su Roles.java está en la etiqueta de git monolito-final).
 */
public final class Roles {
    private Roles() {}

    /** Personal interno: puede consultar. */
    public static final String PERSONAL = "hasAnyRole('ADMIN','AGENTE','ACTUARIO')";

    /** Operación comercial: crear y cambiar clientes y vehículos, consultar placas. */
    public static final String OPERACION = "hasAnyRole('ADMIN','AGENTE')";

    public static final String ADMIN = "hasRole('ADMIN')";
}
