package com.andinaseguros.interfaceadapters.in.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaVerifyRequest(
        @NotBlank String challengeToken,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String codigo) {}
