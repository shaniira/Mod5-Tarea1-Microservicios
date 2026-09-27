package com.andinaseguros.claims.interfaceadapters.in.rest.security;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andinaseguros.claims.ClaimsServiceApplication;
import com.andinaseguros.claims.entities.enums.EstadoPoliza;
import com.andinaseguros.claims.entities.model.PolizaRef;
import com.andinaseguros.claims.frameworksdrivers.config.SecurityConfig;
import com.andinaseguros.claims.interfaceadapters.in.rest.controller.SiniestroController;
import com.andinaseguros.claims.interfaceadapters.in.rest.exception.GlobalExceptionHandler;
import com.andinaseguros.claims.usecases.port.out.repository.PolizaRefRepository;
import com.andinaseguros.claims.usecases.service.siniestro.ActualizarEstadoSiniestroUseCase;
import com.andinaseguros.claims.usecases.service.siniestro.ListarSiniestrosUseCase;
import com.andinaseguros.claims.usecases.service.siniestro.RegistrarSiniestroUseCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Riesgo S2: mismas reglas que el monolito; el dueño de la póliza sale de policy_ref. */
@WebMvcTest(SiniestroController.class)
@ContextConfiguration(classes = ClaimsServiceApplication.class)
@Import({SecurityConfig.class, AccesoRecursos.class, GlobalExceptionHandler.class})
class ControlAccesoSiniestrosTest {
    @Autowired MockMvc mvc;
    @MockBean RegistrarSiniestroUseCase registrar;
    @MockBean ActualizarEstadoSiniestroUseCase actualizar;
    @MockBean ListarSiniestrosUseCase listar;
    @MockBean PolizaRefRepository polizas;
    @MockBean JwtDecoder jwtDecoder;

    private final UUID ana = UUID.randomUUID();
    private final UUID polizaDeAna = UUID.randomUUID();

    @Test
    void elClienteVeLosSiniestrosDeSuPoliza() throws Exception {
        polizaDeAnaExiste();
        when(listar.execute(polizaDeAna)).thenReturn(List.of());

        mvc.perform(get("/api/polizas/{id}/siniestros", polizaDeAna).with(cliente(ana)))
                .andExpect(status().isOk());
    }

    @Test
    void otroClienteNoLosVe() throws Exception {
        polizaDeAnaExiste();

        mvc.perform(get("/api/polizas/{id}/siniestros", polizaDeAna).with(cliente(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void unaPolizaQueNoEstaEnLaProyeccionTambienEs403ParaElCliente() throws Exception {
        when(polizas.buscarPorId(polizaDeAna)).thenReturn(Optional.empty());

        mvc.perform(get("/api/polizas/{id}/siniestros", polizaDeAna).with(cliente(ana)))
                .andExpect(status().isForbidden());
    }

    @Test
    void elActuarioConsultaPeroNoCambiaEstados() throws Exception {
        when(listar.execute(polizaDeAna)).thenReturn(List.of());

        mvc.perform(get("/api/polizas/{id}/siniestros", polizaDeAna).with(rol("ACTUARIO")))
                .andExpect(status().isOk());
        mvc.perform(
                        patch("/api/polizas/{p}/siniestros/{s}/estado", polizaDeAna, UUID.randomUUID())
                                .with(rol("ACTUARIO"))
                                .contentType("application/json")
                                .content("{\"estado\":\"APROBADO\"}"))
                .andExpect(status().isForbidden());
    }

    private void polizaDeAnaExiste() {
        when(polizas.buscarPorId(polizaDeAna))
                .thenReturn(Optional.of(new PolizaRef(polizaDeAna, ana, "POL-1", EstadoPoliza.VIGENTE)));
    }

    private static RequestPostProcessor cliente(UUID customerId) {
        return jwt().jwt(j -> j.claim("rol", "CLIENTE").claim("customerId", customerId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    private static RequestPostProcessor rol(String rol) {
        return jwt().jwt(j -> j.claim("rol", rol)).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
