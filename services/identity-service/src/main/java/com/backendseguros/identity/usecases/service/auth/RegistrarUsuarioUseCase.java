package com.backendseguros.identity.usecases.service.auth;

import com.backendseguros.identity.entities.enums.RolUsuario;
import com.backendseguros.identity.usecases.dto.CrearUsuarioRequestModel;
import com.backendseguros.identity.entities.exception.ReglaNegocioException;
import com.backendseguros.identity.entities.model.Usuario;
import com.backendseguros.identity.usecases.port.out.repository.UsuarioRepository;
import com.backendseguros.identity.usecases.port.out.id.IdGeneratorPort;
import com.backendseguros.identity.usecases.port.out.security.PasswordEncoderPort;

/**
 * Alta de usuarios (riesgo S1 corregido): cualquiera puede registrarse, pero solo como CLIENTE. Crear
 * cuentas de personal (ADMIN, AGENTE, ACTUARIO) exige que quien lo pide sea un ADMIN autenticado.
 */
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

    /** Alta pública: siempre CLIENTE. */
    public void execute(CrearUsuarioRequestModel solicitud) {
        execute(solicitud, false);
    }

    public void execute(CrearUsuarioRequestModel solicitud, boolean solicitanteEsAdmin) {
        RolUsuario rol = solicitud.rol() == null ? RolUsuario.CLIENTE : solicitud.rol();
        if (rol != RolUsuario.CLIENTE && !solicitanteEsAdmin) {
            throw new ReglaNegocioException(
                    "ROL_NO_PERMITIDO", "Solo un administrador puede crear cuentas con el rol " + rol);
        }
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
                        rol,
                        true,
                        null,
                        false));
    }
}
