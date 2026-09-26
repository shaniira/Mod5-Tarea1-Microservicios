package com.andinaseguros.identity.usecases.dto;

public record MfaVerifyRequestModel(String challengeToken, String codigo) {}
