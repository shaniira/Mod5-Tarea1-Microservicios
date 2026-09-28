package com.backendseguros.policy.interfaceadapters.in.rest.security;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backendseguros.policy.PolicyServiceApplication;
import com.backendseguros.policy.entities.enums.EstadoPoliza;
import com.backendseguros.policy.entities.model.Poliza;
import com.backendseguros.policy.entities.valueobject.Dinero;
import com.backendseguros.policy.entities.valueobject.PeriodoVigencia;
import com.backendseguros.policy.frameworksdrivers.config.SecurityConfig;
import com.backendseguros.policy.interfaceadapters.in.rest.controller.MiCuentaController;
import com.backendseguros.policy.interfaceadapters.in.rest.controller.PolizaController;
import com.backendseguros.policy.interfaceadapters.in.rest.exception.GlobalExceptionHandler;
import com.backendseguros.policy.usecases.port.out.repository.PolizaRepository;
import com.backendseguros.policy.usecases.service.poliza.EmitirPolizaUseCase;
import com.backendseguros.policy.usecases.service.poliza.ListarMisPolizasUseCase;
import com.backendseguros.policy.usecases.service.poliza.ListarPolizasUseCase;
import com.backendseguros.policy.usecases.service.poliza.ObtenerPolizaUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
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

/** Riesgo S2: las mismas reglas que ControlAccesoPolizasTest del monolito. */
@WebMvcTest({PolizaController.class, MiCuentaController.class})
@ContextConfiguration(classes = PolicyServiceApplication.class)
@Import({SecurityConfig.class, AccesoRecursos.class, GlobalExceptionHandler.class})
class ControlAccesoPolizasTest {
    @Autowired MockMvc mvc;
    @MockBean EmitirPolizaUseCase emitir;
    @MockBean ObtenerPolizaUseCase obtener;
    @MockBean ListarPolizasUseCase listar;
    @MockBean ListarMisPolizasUseCase misPolizas;
    @MockBean PolizaRepository polizas;
    @MockBean JwtDecoder jwtDecoder;

    private final UUID ana = UUID.randomUUID();
    private final UUID polizaDeAna = UUID.randomUUID();

    @Test
    void elClienteVeSuPolizaPeroNoLaDeOtro() throws Exception {
        when(polizas.buscarPorId(polizaDeAna))
                .thenReturn(
                        Optional.of(
                                new Poliza(
                                        polizaDeAna, "POL-1", UUID.randomUUID(), ana, UUID.randomUUID(),
                                        Dinero.soles(BigDecimal.TEN),
                                        new PeriodoVigencia(LocalDate.now(), LocalDate.now().plusYears(1)),
                                        EstadoPoliza.VIGENTE, null, 1)));

        mvc.perform(get("/api/polizas/{id}", polizaDeAna).with(cliente(ana))).andExpect(status().isOk());
        mvc.perform(get("/api/polizas/{id}", polizaDeAna).with(cliente(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void elActuarioConsultaPeroNoEmite() throws Exception {
        when(listar.execute(null)).thenReturn(List.of());
        mvc.perform(get("/api/polizas").with(rol("ACTUARIO"))).andExpect(status().isOk());
        mvc.perform(
                        post("/api/polizas")
                                .with(rol("ACTUARIO"))
                                .contentType("application/json")
                                .content("{\"cotizacionId\":\"" + UUID.randomUUID() + "\",\"inicioVigencia\":\"2026-10-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void miCuentaEsSoloDelClienteYUsaElCustomerIdDelToken() throws Exception {
        when(misPolizas.execute(ana)).thenReturn(List.of());
        mvc.perform(get("/api/mi-cuenta/polizas").with(cliente(ana))).andExpect(status().isOk());
        mvc.perform(get("/api/mi-cuenta/polizas").with(rol("ADMIN"))).andExpect(status().isForbidden());
    }

    /** Fase 7: un id que no es UUID o un JSON roto es un error del cliente, no un 500. */
    @Test
    void unaSolicitudMalFormadaEs400YNo500() throws Exception {
        mvc.perform(get("/api/polizas/{id}", "no-es-uuid").with(rol("ADMIN"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/polizas").with(rol("ADMIN")).contentType("application/json").content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/polizas/{id}/no-existe", UUID.randomUUID()).with(rol("ADMIN")))
                .andExpect(status().isNotFound());
    }

    private static RequestPostProcessor cliente(UUID customerId) {
        return jwt().jwt(j -> j.claim("rol", "CLIENTE").claim("customerId", customerId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    private static RequestPostProcessor rol(String rol) {
        return jwt().jwt(j -> j.claim("rol", rol)).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
