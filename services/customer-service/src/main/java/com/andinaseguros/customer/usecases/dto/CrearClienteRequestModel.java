package com.andinaseguros.customer.usecases.dto;

import java.time.LocalDate;

public record CrearClienteRequestModel(
        String tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String correo,
        String telefono) {}
