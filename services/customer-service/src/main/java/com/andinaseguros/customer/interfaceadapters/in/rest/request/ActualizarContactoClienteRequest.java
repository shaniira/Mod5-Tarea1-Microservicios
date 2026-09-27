package com.andinaseguros.customer.interfaceadapters.in.rest.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record ActualizarContactoClienteRequest(
        @Email String correo,
        @Pattern(regexp = "^\\+?[0-9 ]{6,20}$", message = "Teléfono inválido") String telefono) {}
