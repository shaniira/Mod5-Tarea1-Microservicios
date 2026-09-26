package com.andinaseguros.identity.interfaceadapters.in.rest.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
