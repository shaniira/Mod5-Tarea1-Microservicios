package com.andinaseguros.interfaceadapters.in.rest.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CrearClienteRequest(
        @NotBlank String tipoDocumento,
        @NotBlank String numeroDocumento,
        @NotBlank String nombres,
        @NotBlank String apellidos,
        @NotNull LocalDate fechaNacimiento,
        @Email String correo,
        String telefono) {}
