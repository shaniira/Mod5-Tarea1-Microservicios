package com.andinaseguros.entities.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Cliente {
    private final UUID id;
    private final String tipoDocumento;
    private final String numeroDocumento;
    private final String nombres;
    private final String apellidos;
    private final LocalDate fechaNacimiento;
    private final String correo;
    private final String telefono;
    private final boolean activo;
    private final long version;

    public Cliente(
            UUID id,
            String tipoDocumento,
            String numeroDocumento,
            String nombres,
            String apellidos,
            LocalDate fechaNacimiento,
            String correo,
            String telefono,
            boolean activo) {
        this(
                id,
                tipoDocumento,
                numeroDocumento,
                nombres,
                apellidos,
                fechaNacimiento,
                correo,
                telefono,
                activo,
                1);
    }

    public Cliente(
            UUID id,
            String tipoDocumento,
            String numeroDocumento,
            String nombres,
            String apellidos,
            LocalDate fechaNacimiento,
            String correo,
            String telefono,
            boolean activo,
            long version) {
        if (numeroDocumento == null || numeroDocumento.isBlank())
            throw new IllegalArgumentException("Documento obligatorio");
        if (nombres == null || nombres.isBlank())
            throw new IllegalArgumentException("Nombres obligatorios");
        if (fechaNacimiento == null || fechaNacimiento.isAfter(LocalDate.now().minusYears(18)))
            throw new IllegalArgumentException("El cliente debe ser mayor de edad");
        this.id = id;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.fechaNacimiento = fechaNacimiento;
        this.correo = correo;
        this.telefono = telefono;
        this.activo = activo;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public boolean isActivo() {
        return activo;
    }

    /**
     * Versión del agregado: sube en cada cambio y viaja en los eventos customer.* para que los
     * consumidores descarten los mensajes más viejos que la copia que ya tienen.
     */
    public long getVersion() {
        return version;
    }

    public boolean tieneMismoContacto(String nuevoCorreo, String nuevoTelefono) {
        return Objects.equals(correo, nuevoCorreo) && Objects.equals(telefono, nuevoTelefono);
    }

    public Cliente conContacto(String nuevoCorreo, String nuevoTelefono) {
        return new Cliente(
                id,
                tipoDocumento,
                numeroDocumento,
                nombres,
                apellidos,
                fechaNacimiento,
                nuevoCorreo,
                nuevoTelefono,
                activo,
                version + 1);
    }

    public int edad() {
        return LocalDate.now().getYear()
                - fechaNacimiento.getYear()
                - (LocalDate.now().getDayOfYear() < fechaNacimiento.getDayOfYear() ? 1 : 0);
    }
}
