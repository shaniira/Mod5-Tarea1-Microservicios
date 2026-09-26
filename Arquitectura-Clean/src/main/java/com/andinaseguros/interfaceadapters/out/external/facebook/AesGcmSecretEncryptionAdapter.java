package com.andinaseguros.interfaceadapters.out.external.facebook;

import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.usecases.port.out.security.SecretEncryptionPort;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesGcmSecretEncryptionAdapter implements SecretEncryptionPort {
    private final String encodedKey;
    private final SecureRandom random = new SecureRandom();

    public AesGcmSecretEncryptionAdapter(String encodedKey) { this.encodedKey = encodedKey; }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new ReglaNegocioException("FACEBOOK_TOKEN_INVALIDO", "Facebook no devolvió un token válido");
        }
        try {
            byte[] key = Base64.getDecoder().decode(encodedKey);
            if (key.length != 32) throw new IllegalArgumentException();
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(iv) + "." + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception exception) {
            throw new ReglaNegocioException("FACEBOOK_CONFIGURACION_INVALIDA", "La protección de tokens Facebook no está configurada");
        }
    }
}