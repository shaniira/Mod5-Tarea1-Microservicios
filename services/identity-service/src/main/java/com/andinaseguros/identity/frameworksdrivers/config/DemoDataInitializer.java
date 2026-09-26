package com.andinaseguros.identity.frameworksdrivers.config;

import com.andinaseguros.identity.entities.enums.RolUsuario;
import com.andinaseguros.identity.entities.model.Usuario;
import com.andinaseguros.identity.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.identity.usecases.port.out.security.PasswordEncoderPort;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Crea el usuario demo "admin" si no existe, igual que hacía el monolito. Solo con
 * app.demo-data.enabled=true (en Compose, entorno local); en otro entorno los usuarios llegan por
 * la migración de la colección usuarios.
 */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {
    private final UsuarioRepository usuarios;
    private final PasswordEncoderPort passwordEncoder;

    public DemoDataInitializer(UsuarioRepository usuarios, PasswordEncoderPort passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.buscarPorUsername("admin").isEmpty()) {
            usuarios.guardar(
                    new Usuario(
                            UUID.fromString("00000000-0000-0000-0000-000000000001"),
                            "admin",
                            null,
                            passwordEncoder.codificar("Admin123*"),
                            null,
                            RolUsuario.ADMIN,
                            true,
                            null,
                            false));
        }
    }
}
