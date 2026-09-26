package com.andinaseguros.interfaceadapters.in.rest.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andinaseguros.entities.model.Poliza;
import com.andinaseguros.frameworksdrivers.configuration.spring.SecurityConfig;
import com.andinaseguros.interfaceadapters.in.rest.controller.PolizaController;
import com.andinaseguros.usecases.port.in.EmitirPolizaInputPort;
import com.andinaseguros.usecases.port.out.repository.CotizacionRepository;
import com.andinaseguros.usecases.port.out.repository.PolizaRepository;
import com.andinaseguros.usecases.service.poliza.ListarPolizasUseCase;
import com.andinaseguros.usecases.service.poliza.ObtenerPolizaUseCase;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import com.andinaseguros.frameworksdrivers.bootstrap.MotorTarificacionApplication;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

/** Riesgo S2: reglas por rol y por propietario con la cadena real de Spring Security. */
@WebMvcTest(PolizaController.class)
@ContextConfiguration(classes = MotorTarificacionApplication.class)
@Import({SecurityConfig.class, AccesoRecursos.class})
class ControlAccesoPolizasTest {
    @Autowired MockMvc mvc;
    @MockBean EmitirPolizaInputPort emitir;
    @MockBean ObtenerPolizaUseCase obtener;
    @MockBean ListarPolizasUseCase listar;
    @MockBean PolizaRepository polizas;
    @MockBean CotizacionRepository cotizaciones;
    @MockBean JwtDecoder jwtDecoder;

    private final UUID clienteAna = UUID.randomUUID();
    private final UUID polizaDeAna = UUID.randomUUID();

    private void polizaDeAnaExiste() {
        Poliza poliza = Mockito.mock(Poliza.class);
        when(poliza.getClienteId()).thenReturn(clienteAna);
        when(polizas.buscarPorId(polizaDeAna)).thenReturn(Optional.of(poliza));
    }

    @Test
    void elClienteVeSuPropiaPoliza() throws Exception {
        polizaDeAnaExiste();
        mvc.perform(get("/api/polizas/{id}", polizaDeAna).with(cliente(clienteAna)))
                .andExpect(status().isOk());
    }

    @Test
    void unClienteNoVeLaPolizaDeOtro() throws Exception {
        polizaDeAnaExiste();
        mvc.perform(get("/api/polizas/{id}", polizaDeAna).with(cliente(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void unClienteNoPuedeListarNiEmitirPolizas() throws Exception {
        mvc.perform(get("/api/polizas").with(cliente(clienteAna))).andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/polizas")
                                .with(cliente(clienteAna))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cotizacionId\":\"" + UUID.randomUUID() + "\",\"inicioVigencia\":\"2026-10-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elPersonalVeCualquierPolizaYElActuarioNoEmite() throws Exception {
        mvc.perform(get("/api/polizas/{id}", polizaDeAna).with(personal("AGENTE"))).andExpect(status().isOk());
        mvc.perform(get("/api/polizas").with(personal("ACTUARIO"))).andExpect(status().isOk());
        mvc.perform(
                        post("/api/polizas")
                                .with(personal("ACTUARIO"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cotizacionId\":\"" + UUID.randomUUID() + "\",\"inicioVigencia\":\"2026-10-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenEs401() throws Exception {
        mvc.perform(get("/api/polizas/{id}", polizaDeAna)).andExpect(status().isUnauthorized());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor cliente(UUID customerId) {
        return jwt().jwt(j -> j.subject("cliente@x.com").claim("rol", "CLIENTE").claim("customerId", customerId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor personal(String rol) {
        return jwt().jwt(j -> j.subject(rol.toLowerCase()).claim("rol", rol))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
