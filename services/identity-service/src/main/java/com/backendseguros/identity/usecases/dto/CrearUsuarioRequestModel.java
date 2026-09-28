package com.backendseguros.identity.usecases.dto;

import com.backendseguros.identity.entities.enums.RolUsuario;

public record CrearUsuarioRequestModel(String username, String password, RolUsuario rol) {}
