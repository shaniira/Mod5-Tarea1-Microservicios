package com.andinaseguros.interfaceadapters.in.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.andinaseguros.interfaceadapters.in.rest.request.CrearUsuarioRequest;
import com.andinaseguros.interfaceadapters.in.rest.request.GoogleLoginRequest;
import com.andinaseguros.interfaceadapters.in.rest.request.LoginRequest;
import com.andinaseguros.interfaceadapters.out.external.facebook.FacebookProperties;
import com.andinaseguros.usecases.dto.CrearUsuarioRequestModel;
import com.andinaseguros.usecases.dto.GoogleLoginRequestModel;
import com.andinaseguros.usecases.dto.LoginRequestModel;
import com.andinaseguros.usecases.dto.Responses.TokenResponse;
import com.andinaseguros.usecases.dto.Responses.ResultadoLogin;
import com.andinaseguros.usecases.service.auth.*;
import com.andinaseguros.usecases.port.out.security.LoginTicketPort;
import com.andinaseguros.entities.enums.RolUsuario;
import org.junit.jupiter.api.Test;

class AuthControllerTest {
    @Test
    void controllerDelegaEnCasosDeUsoSinAccederAJpa() {
        var registro = mock(RegistrarUsuarioUseCase.class);
        var autenticacion = mock(AutenticarUsuarioUseCase.class);
        var autenticacionGoogle = mock(AutenticarConGoogleUseCase.class);
        var verificarMfa = mock(VerificarMfaUseCase.class);
        var facebook = mock(AutenticarConFacebookUseCase.class);
        var desvincularFacebook = mock(DesvincularFacebookUseCase.class);
        var obtenerPerfil = mock(ObtenerPerfilUseCase.class);
        var loginTickets = mock(LoginTicketPort.class);
        var facebookProperties = mock(FacebookProperties.class);
        var controller = new AuthController(
            registro,
            autenticacion,
            autenticacionGoogle,
            verificarMfa,
            facebook,
            desvincularFacebook,
            obtenerPerfil,
            loginTickets,
            facebookProperties);
        var crear = new CrearUsuarioRequest("operador", "secreto", RolUsuario.ADMIN);
        var login = new LoginRequest("operador", "secreto");
        var crearCore = new CrearUsuarioRequestModel("operador", "secreto", RolUsuario.ADMIN);
        var loginCore = new LoginRequestModel("operador", "secreto");
        var token = new TokenResponse("jwt", "Bearer", 3600);
        var resultado = ResultadoLogin.exitoso(token);
        when(autenticacion.execute(loginCore)).thenReturn(resultado);
        var google = new GoogleLoginRequest("id-token");
        var googleCore = new GoogleLoginRequestModel("id-token");
        var tokenGoogle = new TokenResponse("jwt-google", "Bearer", 3600);
        when(autenticacionGoogle.execute(googleCore)).thenReturn(tokenGoogle);

        assertThat(controller.register(crear).getStatusCode().value()).isEqualTo(201);
        assertThat(controller.login(login)).isEqualTo(resultado);
        assertThat(controller.google(google)).isEqualTo(tokenGoogle);
        verify(registro).execute(crearCore);
        verify(autenticacion).execute(loginCore);
        verify(autenticacionGoogle).execute(googleCore);
    }

    @Test
    void callbackFacebookRedirigeConTicketYElCanjeDevuelveElJwt() {
        var registro = mock(RegistrarUsuarioUseCase.class);
        var autenticacion = mock(AutenticarUsuarioUseCase.class);
        var autenticacionGoogle = mock(AutenticarConGoogleUseCase.class);
        var verificarMfa = mock(VerificarMfaUseCase.class);
        var facebook = mock(AutenticarConFacebookUseCase.class);
        var desvincularFacebook = mock(DesvincularFacebookUseCase.class);
        var obtenerPerfil = mock(ObtenerPerfilUseCase.class);
        var loginTickets = mock(LoginTicketPort.class);
        var facebookProperties = mock(FacebookProperties.class);
        var controller = new AuthController(
                registro,
                autenticacion,
                autenticacionGoogle,
                verificarMfa,
                facebook,
                desvincularFacebook,
                obtenerPerfil,
                loginTickets,
                facebookProperties);
        var token = new TokenResponse("jwt", "Bearer", 3600);
        when(facebook.callback("code", "state")).thenReturn(token);
        when(loginTickets.create(token)).thenReturn("temporary-ticket");
        when(facebookProperties.frontendCallbackUrl()).thenReturn("http://localhost:5173/login");
        when(loginTickets.consume("temporary-ticket")).thenReturn(java.util.Optional.of(token));

        var callback = controller.facebookCallback("code", "state", null);

        assertThat(callback.getStatusCode().value()).isEqualTo(302);
        assertThat(callback.getHeaders().getLocation())
                .hasToString("http://localhost:5173/login?ticket=temporary-ticket");
        assertThat(controller.facebookSession("temporary-ticket")).isEqualTo(token);
    }
}
