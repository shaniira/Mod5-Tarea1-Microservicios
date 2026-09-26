package com.andinaseguros.usecases.dto;

public record MfaVerifyRequestModel(String challengeToken, String codigo) {}
