package com.andinaseguros.quotation.interfaceadapters.in.rest.security;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andinaseguros.quotation.QuotationServiceApplication;
import com.andinaseguros.quotation.entities.enums.EstadoCotizacion;
import com.andinaseguros.quotation.entities.model.Cotizacion;
import com.andinaseguros.quotation.entities.valueobject.Dinero;
import com.andinaseguros.quotation.frameworksdrivers.config.SecurityConfig;
import com.andinaseguros.quotation.interfaceadapters.in.rest.controller.CotizacionController;
import com.andinaseguros.quotation.interfaceadapters.in.rest.exception.GlobalExceptionHandler;
import com.andinaseguros.quotation.usecases.exception.ClientesNoDisponiblesException;
import com.andinaseguros.quotation.usecases.port.in.CrearCotizacionInputPort;
import com.andinaseguros.quotation.usecases.port.out.repository.CotizacionRepository;
import com.andinaseguros.quotation.usecases.service.cotizacion.AceptarCotizacionUseCase;
import com.andinaseguros.quotation.usecases.service.cotizacion.ListarCotizacionesPendientesEmisionUseCase;
import com.andinaseguros.quotation.usecases.service.cotizacion.ListarCotizacionesUseCase;
import com.andinaseguros.quotation.usecases.service.cotizacion.ObtenerCotizacionUseCase;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Riesgo S2 y paso 5.3: mismas reglas que el monolito y 503 claro si customer-service no responde. */
@WebMvcTest(CotizacionController.class)
@ContextConfiguration(classes = QuotationServiceApplication.class)
@Import({SecurityConfig.class, AccesoRecursos.class, GlobalExceptionHandler.class})
class ControlAccesoCotizacionesTest {
    @Autowired MockMvc mvc;
    @MockBean CrearCotizacionInputPort crear;
    @MockBean ObtenerCotizacionUseCase obtener;
    @MockBean AceptarCotizacionUseCase aceptar;
    @MockBean ListarCotizacionesUseCase listar;
    @MockBean ListarCotizacionesPendientesEmisionUseCase pendientes;
    @MockBean CotizacionRepository cotizaciones;
    @MockBean JwtDecoder jwtDecoder;

    private final UUID ana = UUID.randomUUID();
    private final UUID cotizacionDeAna = UUID.randomUUID();

    @Test
    void elClienteVeSuCotizacionPeroNoLaDeOtro() throws Exception {
        when(cotizaciones.buscarPorId(cotizacionDeAna))
                .thenReturn(
                        Optional.of(
                                new Cotizacion(
                                        cotizacionDeAna, "COT-1", ana, UUID.randomUUID(), UUID.randomUUID(),
                                        Dinero.soles(BigDecimal.TEN), LocalDateTime.now(), LocalDateTime.now().plusDays(15),
                                        EstadoCotizacion.VIGENTE)));

        mvc.perform(get("/api/cotizaciones/{id}", cotizacionDeAna).with(cliente(ana))).andExpect(status().isOk());
        mvc.perform(get("/api/cotizaciones/{id}", cotizacionDeAna).with(cliente(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void elActuarioNoAceptaCotizaciones() throws Exception {
        mvc.perform(patch("/api/cotizaciones/{id}/aceptar", UUID.randomUUID()).with(rol("ACTUARIO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void conCustomerServiceCaidoRespondeUn503Claro() throws Exception {
        when(crear.execute(ArgumentMatchers.any()))
                .thenThrow(new ClientesNoDisponiblesException(new RuntimeException("timeout")));

        mvc.perform(
                        post("/api/cotizaciones")
                                .with(rol("AGENTE"))
                                .contentType("application/json")
                                .content("{\"clienteId\":\"" + ana + "\",\"vehiculoId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.codigo").value("CLIENTES_NO_DISPONIBLE"));
    }

    /** Fase 7: un id que no es UUID o un JSON roto es un error del cliente, no un 500. */
    @Test
    void unaSolicitudMalFormadaEs400YNo500() throws Exception {
        mvc.perform(get("/api/cotizaciones/{id}", "no-es-uuid").with(rol("ADMIN"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/cotizaciones").with(rol("ADMIN")).contentType("application/json").content("{"))
                .andExpect(status().isBadRequest());
    }

    private static RequestPostProcessor cliente(UUID customerId) {
        return jwt().jwt(j -> j.claim("rol", "CLIENTE").claim("customerId", customerId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    private static RequestPostProcessor rol(String rol) {
        return jwt().jwt(j -> j.claim("rol", rol)).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
