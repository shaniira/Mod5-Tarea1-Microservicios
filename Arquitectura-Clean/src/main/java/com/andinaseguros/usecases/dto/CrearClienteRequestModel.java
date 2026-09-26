package com.andinaseguros.usecases.dto;

import java.time.LocalDate;

public record CrearClienteRequestModel(
        String tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String correo,
        String telefono) {}
