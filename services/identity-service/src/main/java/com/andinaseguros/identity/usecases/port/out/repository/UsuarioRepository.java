package com.andinaseguros.identity.usecases.port.out.repository;

import com.andinaseguros.identity.entities.model.Usuario;
import java.util.Optional;

public interface UsuarioRepository {
    Usuario guardar(Usuario usuario);

    Optional<Usuario> buscarPorUsername(String username);

    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorGoogleSubject(String googleSubject);

    Optional<Usuario> buscarPorProveedorYProveedorUsuarioId(String provider, String providerUserId);
}
