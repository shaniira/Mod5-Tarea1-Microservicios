package com.andinaseguros.identity.interfaceadapters.in.rest.request;

import com.andinaseguros.identity.entities.enums.RolUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearUsuarioRequest(
        @NotBlank String username, @NotBlank String password, RolUsuario rol) {}
