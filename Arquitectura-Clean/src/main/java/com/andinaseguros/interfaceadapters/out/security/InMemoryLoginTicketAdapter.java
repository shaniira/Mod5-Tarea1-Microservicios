package com.andinaseguros.interfaceadapters.out.security;

import com.andinaseguros.usecases.dto.Responses.TokenResponse;
import com.andinaseguros.usecases.port.out.security.LoginTicketPort;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLoginTicketAdapter implements LoginTicketPort {
    private final ConcurrentHashMap<String, Entry> tickets = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final long expirationSeconds;

    public InMemoryLoginTicketAdapter(long expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
    }

    public String create(TokenResponse response) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tickets.put(ticket, new Entry(response, Instant.now().plusSeconds(expirationSeconds)));
        return ticket;
    }

    public Optional<TokenResponse> consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return Optional.empty();
        }
        Entry entry = tickets.remove(ticket);
        return entry != null && entry.expiresAt().isAfter(Instant.now())
                ? Optional.of(entry.response())
                : Optional.empty();
    }

    private record Entry(TokenResponse response, Instant expiresAt) {}
}