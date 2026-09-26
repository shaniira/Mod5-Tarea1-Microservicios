package com.andinaseguros.interfaceadapters.out.security.mfa;

import com.andinaseguros.usecases.port.out.security.MfaSecretGeneratorPort;
import com.andinaseguros.usecases.port.out.security.TotpVerifierPort;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TotpSecurityAdapter implements MfaSecretGeneratorPort, TotpVerifierPort {
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generar() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    @Override
    public boolean verificar(String secret, String codigo) {
        if (secret == null || codigo == null || !codigo.matches("\\d{6}")) return false;
        long counter = System.currentTimeMillis() / 30_000L;
        for (long offset = -1; offset <= 1; offset++) {
            if (codigo.equals(generarCodigo(secret, counter + offset))) return true;
        }
        return false;
    }

    private String generarCodigo(String secret, long counter) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decodeBase32(secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
            return String.format("%06d", binary % 1_000_000);
        } catch (Exception e) {
            return "";
        }
    }

    private String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte value : data) {
            buffer = (buffer << 8) | (value & 0xff);
            bits += 8;
            while (bits >= 5) {
                result.append(BASE32.charAt((buffer >> (bits -= 5)) & 31));
            }
        }
        if (bits > 0) result.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        return result.toString();
    }

    private byte[] decodeBase32(String value) {
        String clean = value.replace("=", "").replace(" ", "").toUpperCase();
        byte[] output = new byte[clean.length() * 5 / 8];
        int buffer = 0, bits = 0, index = 0;
        for (char character : clean.toCharArray()) {
            int digit = BASE32.indexOf(character);
            if (digit < 0) throw new IllegalArgumentException("Secreto Base32 inválido");
            buffer = (buffer << 5) | digit;
            bits += 5;
            if (bits >= 8) output[index++] = (byte) ((buffer >> (bits -= 8)) & 0xff);
        }
        return output;
    }
}
