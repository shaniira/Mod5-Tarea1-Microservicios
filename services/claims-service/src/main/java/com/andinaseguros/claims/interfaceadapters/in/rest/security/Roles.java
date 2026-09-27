package com.andinaseguros.claims.interfaceadapters.in.rest.security;

/**
 * Reglas de acceso por rol (riesgo S2), las mismas que tenía el monolito para siniestros
 * (Arquitectura-Clean/.../security/Roles.java).
 */
public final class Roles {
    private Roles() {}

    /** Personal interno: puede consultar. */
    public static final String PERSONAL = "hasAnyRole('ADMIN','AGENTE','ACTUARIO')";

    /** Operación comercial: registrar siniestros y cambiar su estado. */
    public static final String OPERACION = "hasAnyRole('ADMIN','AGENTE')";

    public static final String ADMIN = "hasRole('ADMIN')";
}
