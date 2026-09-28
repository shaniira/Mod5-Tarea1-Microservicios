package com.backendseguros.identity.usecases.port.out.security;

public interface SecretEncryptionPort {
    String encrypt(String plainText);
}