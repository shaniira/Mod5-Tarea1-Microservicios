package com.backendseguros.identity.usecases.port.out.security;

import com.backendseguros.identity.usecases.dto.Responses.TokenResponse;
import java.util.Optional;

public interface LoginTicketPort {
    String create(TokenResponse response);

    Optional<TokenResponse> consume(String ticket);
}