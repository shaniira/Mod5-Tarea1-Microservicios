package com.andinaseguros.interfaceadapters.in.rest.request;

import com.andinaseguros.entities.enums.RolUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearUsuarioRequest(
        @NotBlank String username, @NotBlank String password, @NotNull RolUsuario rol) {}
