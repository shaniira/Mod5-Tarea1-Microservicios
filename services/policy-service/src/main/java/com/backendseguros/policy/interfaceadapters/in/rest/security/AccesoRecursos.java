package com.backendseguros.policy.interfaceadapters.in.rest.security;

import com.backendseguros.policy.usecases.port.out.repository.PolizaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Control de propiedad (riesgo S2): un CLIENTE solo ve sus pólizas y su historial de renovaciones.
 * El cliente del usuario sale del claim customerId del token. Si la póliza no existe se responde
 * 403, igual que el monolito.
 */
@Component("acceso")
public class AccesoRecursos {
    private final PolizaRepository polizas;

    public AccesoRecursos(PolizaRepository polizas) {
        this.polizas = polizas;
    }

    public boolean esDuenoDePoliza(UUID polizaId) {
        return clienteDelToken()
                .flatMap(propio -> polizas.buscarPorId(polizaId).map(p -> propio.equals(p.getClienteId())))
                .orElse(false);
    }

    /** customerId del token, solo si el usuario es CLIENTE. */
    public Optional<UUID> clienteDelToken() {
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
