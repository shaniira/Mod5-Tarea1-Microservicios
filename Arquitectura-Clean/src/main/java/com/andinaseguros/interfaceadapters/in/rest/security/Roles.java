package com.andinaseguros.interfaceadapters.in.rest.security;

/**
 * Reglas de acceso por rol (riesgo S2), en un solo lugar. Coinciden con lo que muestra el frontend
 * a cada rol (frontend/src/router).
 */
public final class Roles {
    private Roles() {}

    /** Personal interno: puede consultar (el Resumen lo ven ADMIN, AGENTE y ACTUARIO). */
    public static final String PERSONAL = "hasAnyRole('ADMIN','AGENTE','ACTUARIO')";

    /** Operación comercial: crear y cambiar clientes, cotizaciones, pólizas, siniestros... */
    public static final String OPERACION = "hasAnyRole('ADMIN','AGENTE')";

    /** Tablas tarifarias: las mantienen ADMIN y ACTUARIO. */
    public static final String TARIFAS = "hasAnyRole('ADMIN','ACTUARIO')";

    public static final String ADMIN = "hasRole('ADMIN')";
    public static final String CLIENTE = "hasRole('CLIENTE')";
}
