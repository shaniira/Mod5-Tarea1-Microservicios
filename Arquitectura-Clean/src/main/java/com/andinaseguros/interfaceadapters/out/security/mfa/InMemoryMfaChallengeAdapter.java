package com.andinaseguros.interfaceadapters.out.security.mfa;

import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.usecases.port.out.security.*;
import com.andinaseguros.usecases.port.out.time.ClockPort;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMfaChallengeAdapter implements MfaChallengePort {
    private record Stored(AuthenticatedUser usuario, Instant expira) {}

    private final ConcurrentHashMap<String, Stored> desafios = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final ClockPort clock;
    private final long duracion;

    public InMemoryMfaChallengeAdapter(ClockPort clock, long duracion) {
        this.clock = clock;
        this.duracion = duracion;
    }

    @Override
    public MfaChallenge crear(AuthenticatedUser usuario) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        desafios.put(token, new Stored(usuario, clock.now().plusSeconds(duracion)));
        return new MfaChallenge(token, duracion);
    }

    @Override
    public AuthenticatedUser consumir(String token) {
        Stored stored = token == null ? null : desafios.remove(token);
        if (stored == null) throw error("MFA_DESAFIO_INVALIDO", "El desafío MFA no es válido o ya fue utilizado");
        if (!stored.expira().isAfter(clock.now())) throw error("MFA_DESAFIO_EXPIRADO", "El desafío MFA expiró");
        return stored.usuario();
    }

    private ReglaNegocioException error(String codigo, String mensaje) {
        return new ReglaNegocioException(codigo, mensaje);
    }
}
