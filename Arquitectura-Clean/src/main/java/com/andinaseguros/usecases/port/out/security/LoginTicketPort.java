package com.andinaseguros.usecases.port.out.security;

import com.andinaseguros.usecases.dto.Responses.TokenResponse;
import java.util.Optional;

public interface LoginTicketPort {
    String create(TokenResponse response);

    Optional<TokenResponse> consume(String ticket);
}