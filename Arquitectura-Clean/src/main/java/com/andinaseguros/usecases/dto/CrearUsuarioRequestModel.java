package com.andinaseguros.usecases.dto;

import com.andinaseguros.entities.enums.RolUsuario;

public record CrearUsuarioRequestModel(String username, String password, RolUsuario rol) {}
