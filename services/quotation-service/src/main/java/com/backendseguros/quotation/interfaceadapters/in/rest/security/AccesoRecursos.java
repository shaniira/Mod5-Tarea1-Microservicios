package com.backendseguros.quotation.interfaceadapters.in.rest.security;

import com.backendseguros.quotation.usecases.port.out.repository.CotizacionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Control de propiedad (riesgo S2): un CLIENTE solo ve sus cotizaciones. El cliente del usuario sale
 * del claim customerId del token. Si la cotización no existe se responde 403, igual que el
 * monolito, para no revelar qué identificadores existen.
 */
@Component("acceso")
public class AccesoRecursos {
    private final CotizacionRepository cotizaciones;

    public AccesoRecursos(CotizacionRepository cotizaciones) {
        this.cotizaciones = cotizaciones;
    }

    public boolean esDuenoDeCotizacion(UUID cotizacionId) {
        return clienteDelToken()
                .flatMap(propio -> cotizaciones.buscarPorId(cotizacionId).map(c -> propio.equals(c.getClienteId())))
                .orElse(false);
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
