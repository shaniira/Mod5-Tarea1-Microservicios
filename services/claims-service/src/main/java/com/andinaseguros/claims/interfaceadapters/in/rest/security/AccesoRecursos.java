package com.andinaseguros.claims.interfaceadapters.in.rest.security;

import com.andinaseguros.claims.usecases.port.out.repository.PolizaRefRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Control de propiedad (riesgo S2): un CLIENTE solo ve los siniestros de sus pólizas. El dueño de
 * la póliza sale de policy_ref (no de la base de pólizas) y el cliente del usuario, del claim
 * customerId del token. Si la póliza no está en la proyección se responde "no es tuya" (403), igual
 * que el monolito, para no revelar qué identificadores existen.
 */
@Component("acceso")
public class AccesoRecursos {
    private final PolizaRefRepository polizas;

    public AccesoRecursos(PolizaRefRepository polizas) {
        this.polizas = polizas;
    }

    public boolean esDuenoDePoliza(UUID polizaId) {
        return clienteDelToken()
                .flatMap(propio -> polizas.buscarPorId(polizaId).map(p -> propio.equals(p.clienteId())))
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
