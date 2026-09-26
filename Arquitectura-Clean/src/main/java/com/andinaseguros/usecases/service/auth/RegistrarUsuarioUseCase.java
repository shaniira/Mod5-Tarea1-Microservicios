package com.andinaseguros.usecases.service.auth;

import com.andinaseguros.usecases.dto.CrearUsuarioRequestModel;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.model.Usuario;
import com.andinaseguros.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.usecases.port.out.security.PasswordEncoderPort;

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
