package com.backendseguros.identity.interfaceadapters.in.rest.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(@NotBlank String idToken) {}
