package com.andinaseguros.interfaceadapters.in.rest.controller;

import com.andinaseguros.interfaceadapters.in.rest.request.*;
import static com.andinaseguros.interfaceadapters.in.rest.mapper.RestRequestMapper.toCore;
import com.andinaseguros.usecases.dto.Responses.TokenResponse;
import com.andinaseguros.usecases.dto.Responses.ResultadoLogin;
import com.andinaseguros.usecases.dto.Responses.PerfilResponse;
import com.andinaseguros.usecases.service.auth.*;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.interfaceadapters.out.external.facebook.FacebookProperties;
import com.andinaseguros.usecases.port.out.security.LoginTicketPort;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final RegistrarUsuarioUseCase registrarUsuario;
    private final AutenticarUsuarioUseCase autenticarUsuario;
    private final AutenticarConGoogleUseCase autenticarConGoogle;
    private final VerificarMfaUseCase verificarMfa;
    private final AutenticarConFacebookUseCase autenticarConFacebook;
    private final DesvincularFacebookUseCase desvincularFacebook;
    private final ObtenerPerfilUseCase obtenerPerfil;
    private final LoginTicketPort loginTickets;
    private final FacebookProperties facebookProperties;

    public AuthController(
            RegistrarUsuarioUseCase registrarUsuario,
            AutenticarUsuarioUseCase autenticarUsuario,
            AutenticarConGoogleUseCase autenticarConGoogle,
            VerificarMfaUseCase verificarMfa,
            AutenticarConFacebookUseCase autenticarConFacebook,
            DesvincularFacebookUseCase desvincularFacebook,
            ObtenerPerfilUseCase obtenerPerfil,
            LoginTicketPort loginTickets,
            FacebookProperties facebookProperties) {
        this.registrarUsuario = registrarUsuario;
        this.autenticarUsuario = autenticarUsuario;
        this.autenticarConGoogle = autenticarConGoogle;
        this.verificarMfa = verificarMfa;
        this.autenticarConFacebook = autenticarConFacebook;
        this.desvincularFacebook = desvincularFacebook;
        this.obtenerPerfil = obtenerPerfil;
        this.loginTickets = loginTickets;
        this.facebookProperties = facebookProperties;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody CrearUsuarioRequest solicitud) {
        registrarUsuario.execute(toCore(solicitud));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResultadoLogin login(@Valid @RequestBody LoginRequest solicitud) {
        return autenticarUsuario.execute(toCore(solicitud));
    }

    @PostMapping("/google")
    public TokenResponse google(@Valid @RequestBody GoogleLoginRequest solicitud) {
        return autenticarConGoogle.execute(toCore(solicitud));
    }

    @PostMapping("/mfa/verificar")
    public TokenResponse verificarMfa(@Valid @RequestBody MfaVerifyRequest solicitud) {
        return verificarMfa.execute(toCore(solicitud));
    }

    @GetMapping("/facebook")
    public ResponseEntity<Void> facebook() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(autenticarConFacebook.iniciar()))
                .build();
    }

    @GetMapping("/facebook/callback")
    public ResponseEntity<Void> facebookCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        if (error != null) {
            throw new ReglaNegocioException("FACEBOOK_AUTORIZACION_RECHAZADA", "La autorización de Facebook fue rechazada");
        }
        TokenResponse response = autenticarConFacebook.callback(code, state);
        String ticket = loginTickets.create(response);
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(frontendCallbackUrl() + "?ticket=" + ticket))
            .build();
        }

        @PostMapping("/facebook/session")
        public TokenResponse facebookSession(@RequestParam String ticket) {
        return loginTickets
            .consume(ticket)
            .orElseThrow(
                () ->
                    new ReglaNegocioException(
                        "FACEBOOK_TICKET_INVALIDO",
                        "La sesión de Facebook expiró o no es válida"));
    }

    @DeleteMapping("/facebook")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> desvincularFacebook(Authentication authentication) {
        desvincularFacebook.execute(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public PerfilResponse me(Authentication authentication) {
        return obtenerPerfil.execute(authentication.getName());
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    private String frontendCallbackUrl() {
        String url = facebookProperties.frontendCallbackUrl();
        if (url == null || url.isBlank()) {
            throw new ReglaNegocioException(
                    "FACEBOOK_CONFIGURACION_INVALIDA", "La URL de retorno del frontend no está configurada");
        }
        return url;
    }
}
