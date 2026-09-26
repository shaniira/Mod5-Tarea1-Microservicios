package com.andinaseguros.usecases.port.out.security;

public interface SecretEncryptionPort {
    String encrypt(String plainText);
}