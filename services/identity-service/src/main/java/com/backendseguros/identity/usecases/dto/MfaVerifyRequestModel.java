package com.backendseguros.identity.usecases.dto;

public record MfaVerifyRequestModel(String challengeToken, String codigo) {}
