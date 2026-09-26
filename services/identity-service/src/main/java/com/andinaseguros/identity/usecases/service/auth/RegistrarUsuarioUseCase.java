package com.andinaseguros.identity.usecases.service.auth;

import com.andinaseguros.identity.usecases.dto.CrearUsuarioRequestModel;
import com.andinaseguros.identity.entities.exception.ReglaNegocioException;
import com.andinaseguros.identity.entities.model.Usuario;
import com.andinaseguros.identity.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.identity.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.identity.usecases.port.out.security.PasswordEncoderPort;

public class RegistrarUsuarioUseCase {
    private final UsuarioRepository usuarios;
    private final PasswordEncoderPort passwordEncoder;
    private final IdGeneratorPort ids;

    public RegistrarUsuarioUseCase(
            UsuarioRepository usuarios, PasswordEncoderPort passwordEncoder, IdGeneratorPort ids) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.ids = ids;
    }

    public void execute(CrearUsuarioRequestModel solicitud) {
        if (usuarios.buscarPorUsername(solicitud.username()).isPresent()) {
            throw new ReglaNegocioException("USUARIO_DUPLICADO", "El usuario ya existe");
        }
        usuarios.guardar(
                new Usuario(
                        ids.generar(),
                        solicitud.username(),
                        null,
                        passwordEncoder.codificar(solicitud.password()),
                        null,
                        solicitud.rol(),
                        true,
                        null,
                        false));
    }
}
