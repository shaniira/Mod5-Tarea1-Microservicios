package com.andinaseguros.interfaceadapters.in.rest.security;

import com.andinaseguros.usecases.port.out.repository.CotizacionRepository;
import com.andinaseguros.usecases.port.out.repository.PolizaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Control de propiedad (riesgo S2): un CLIENTE solo puede ver lo suyo. Se usa desde
 * {@code @PreAuthorize}, por ejemplo {@code @acceso.esDuenoDePoliza(#id)}. El cliente del usuario
 * sale del claim customerId que pone identity-service en el token; no se consulta a identity.
 *
 * <p>Si el recurso no existe se responde "no es tuyo" (403) en vez de 404, para no revelar qué
 * identificadores existen.
 */
@Component("acceso")
public class AccesoRecursos {
    private final PolizaRepository polizas;
    private final CotizacionRepository cotizaciones;

    public AccesoRecursos(PolizaRepository polizas, CotizacionRepository cotizaciones) {
        this.polizas = polizas;
        this.cotizaciones = cotizaciones;
    }

    public boolean esCliente(UUID clienteId) {
        return clienteDelToken().map(propio -> propio.equals(clienteId)).orElse(false);
    }

    public boolean esDuenoDePoliza(UUID polizaId) {
        return clienteDelToken()
                .flatMap(propio -> polizas.buscarPorId(polizaId).map(p -> propio.equals(p.getClienteId())))
                .orElse(false);
    }

    public boolean esDuenoDeCotizacion(UUID cotizacionId) {
        return clienteDelToken()
                .flatMap(
                        propio ->
                                cotizaciones
                                        .buscarPorId(cotizacionId)
                                        .map(c -> propio.equals(c.getClienteId())))
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
