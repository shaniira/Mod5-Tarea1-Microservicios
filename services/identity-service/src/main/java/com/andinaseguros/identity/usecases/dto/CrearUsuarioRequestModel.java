package com.andinaseguros.identity.usecases.dto;

import com.andinaseguros.identity.entities.enums.RolUsuario;

public record CrearUsuarioRequestModel(String username, String password, RolUsuario rol) {}
