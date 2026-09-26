package com.andinaseguros.interfaceadapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.andinaseguros.usecases.dto.Responses.TokenResponse;
import org.junit.jupiter.api.Test;

class InMemoryLoginTicketAdapterTest {
    @Test
    void entregaElJwtUnaSolaVez() {
        var adapter = new InMemoryLoginTicketAdapter(60);
        var response = new TokenResponse("jwt", "Bearer", 3600);

        String ticket = adapter.create(response);

        assertThat(adapter.consume(ticket)).contains(response);
        assertThat(adapter.consume(ticket)).isEmpty();
    }

    @Test
    void rechazaTicketAusente() {
        var adapter = new InMemoryLoginTicketAdapter(60);

        assertThat(adapter.consume(null)).isEmpty();
        assertThat(adapter.consume(" ")).isEmpty();
    }
}