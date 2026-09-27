package com.andinaseguros.quotation.interfaceadapters.in.rest.security;

/**
 * Reglas de acceso por rol (riesgo S2), las mismas que tenía el monolito para tarifas y
 * cotizaciones (Arquitectura-Clean/.../security/Roles.java).
 */
public final class Roles {
    private Roles() {}

    /** Personal interno: puede consultar. */
    public static final String PERSONAL = "hasAnyRole('ADMIN','AGENTE','ACTUARIO')";

    /** Operación comercial: crear, consultar y aceptar cotizaciones. */
    public static final String OPERACION = "hasAnyRole('ADMIN','AGENTE')";

    /** Tablas tarifarias: las mantienen ADMIN y ACTUARIO. */
    public static final String TARIFAS = "hasAnyRole('ADMIN','ACTUARIO')";
}
