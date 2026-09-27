package com.andinaseguros.customer.interfaceadapters.in.rest.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Control de propiedad (riesgo S2): un CLIENTE solo ve su propio registro y sus vehículos. Se usa
 * desde {@code @PreAuthorize("... or @acceso.esCliente(#id)")}. El cliente del usuario sale del
 * claim customerId que pone identity-service en el token; no se consulta a identity.
 */
@Component("acceso")
public class AccesoRecursos {

    public boolean esCliente(UUID clienteId) {
        return clienteDelToken().map(propio -> propio.equals(clienteId)).orElse(false);
    }

    /** customerId del token, solo si el usuario es CLIENTE. */
    private Optional<UUID> clienteDelToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwt)
                || auth.getAuthorities().stream().noneMatch(a -> "ROLE_CLIENTE".equals(a.getAuthority()))) {
            return Optional.empty();
        }
        String customerId = jwt.getToken().getClaimAsString("customerId");
        try {
            return customerId == null ? Optional.empty() : Optional.of(UUID.fromString(customerId));
        } catch (IllegalArgumentException invalido) {
            return Optional.empty();
        }
    }
}
