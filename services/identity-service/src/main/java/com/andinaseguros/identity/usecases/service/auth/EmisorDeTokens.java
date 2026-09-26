package com.andinaseguros.identity.usecases.service.auth;

import com.andinaseguros.identity.entities.enums.RolUsuario;
import com.andinaseguros.identity.entities.model.Usuario;
import com.andinaseguros.identity.usecases.dto.Responses.TokenResponse;
import com.andinaseguros.identity.usecases.port.out.repository.CorreoClienteRepository;
import com.andinaseguros.identity.usecases.port.out.security.AuthenticatedUser;
import com.andinaseguros.identity.usecases.port.out.security.TokenGeneratorPort;
import java.util.Optional;
import java.util.UUID;

/**
 * Arma la identidad que va en el JWT y lo emite. Para un CLIENTE busca su customerId en el índice
 * de correos (por correo del usuario o, si no tiene, por su username cuando es un correo), así los
 * demás servicios saben a qué cliente pertenece sin consultar la base de clientes.
 */
public class EmisorDeTokens {
    private final TokenGeneratorPort tokens;
    private final CorreoClienteRepository correosClientes;

    public EmisorDeTokens(TokenGeneratorPort tokens, CorreoClienteRepository correosClientes) {
        this.tokens = tokens;
        this.correosClientes = correosClientes;
    }

    public AuthenticatedUser identidad(Usuario usuario) {
        String customerId =
                usuario.getRol() == RolUsuario.CLIENTE
                        ? buscarCliente(usuario).map(UUID::toString).orElse(null)
                        : null;
        return new AuthenticatedUser(usuario.getUsername(), usuario.getRol().name(), customerId);
    }

    public TokenResponse emitir(Usuario usuario) {
        return emitir(identidad(usuario));
    }

    public TokenResponse emitir(AuthenticatedUser identidad) {
        return new TokenResponse(tokens.generar(identidad), "Bearer", tokens.expirationSeconds());
    }

    private Optional<UUID> buscarCliente(Usuario usuario) {
        Optional<UUID> porCorreo =
                usuario.getEmail() == null
                        ? Optional.empty()
                        : correosClientes.buscarClientePorCorreo(usuario.getEmail());
        if (porCorreo.isPresent() || usuario.getUsername() == null || !usuario.getUsername().contains("@")) {
            return porCorreo;
        }
        return correosClientes.buscarClientePorCorreo(usuario.getUsername());
    }
}
