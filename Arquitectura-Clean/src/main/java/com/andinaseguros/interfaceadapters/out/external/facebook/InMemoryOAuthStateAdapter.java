package com.andinaseguros.interfaceadapters.out.external.facebook;

import com.andinaseguros.usecases.port.out.facebook.OAuthStatePort;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOAuthStateAdapter implements OAuthStatePort {
    private final ConcurrentHashMap<String, Instant> states = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final int ttlSeconds;

    public InMemoryOAuthStateAdapter(int ttlSeconds) { this.ttlSeconds = ttlSeconds; }

    public String create() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        states.put(state, Instant.now().plusSeconds(ttlSeconds));
        return state;
    }

    public boolean consume(String state) {
        if (state == null) return false;
        Instant expiration = states.remove(state);
        return expiration != null && expiration.isAfter(Instant.now());
    }
}