package com.backendseguros.identity.usecases.dto;

/** Mismas respuestas que tenía el monolito: el frontend no cambia. */
public final class Responses {
    private Responses() {}

    public record TokenResponse(String token, String tipo, long expiraEnSegundos) {}

    public record ResultadoLogin(
            boolean requiresMfa,
            TokenResponse token,
            String challengeToken,
            long challengeExpiresIn) {
        public static ResultadoLogin exitoso(TokenResponse token) {
            return new ResultadoLogin(false, token, null, 0);
        }

        public static ResultadoLogin requiereMfa(String challengeToken, long expiresIn) {
            return new ResultadoLogin(true, null, challengeToken, expiresIn);
        }
    }

    public record MfaSetupResponse(String secret, String otpauthUri, String qrCodeDataUri) {}

    public record PerfilResponse(
            String username, String rol, String nombres, String apellidos, String email) {}

    public record MfaStatusResponse(boolean habilitado) {}
}
