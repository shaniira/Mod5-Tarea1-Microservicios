package com.andinaseguros.customer.interfaceadapters.in.rest.security;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andinaseguros.customer.CustomerServiceApplication;
import com.andinaseguros.customer.frameworksdrivers.config.SecurityConfig;
import com.andinaseguros.customer.interfaceadapters.in.rest.controller.ClienteController;
import com.andinaseguros.customer.interfaceadapters.in.rest.exception.GlobalExceptionHandler;
import com.andinaseguros.customer.usecases.dto.Responses.ClienteResponse;
import com.andinaseguros.customer.usecases.port.in.RegistrarClienteUseCase;
import com.andinaseguros.customer.usecases.port.in.RegistrarVehiculoUseCase;
import com.andinaseguros.customer.usecases.service.cliente.ActualizarContactoClienteUseCase;
import com.andinaseguros.customer.usecases.service.cliente.ListarClientesUseCase;
import com.andinaseguros.customer.usecases.service.cliente.ObtenerClienteUseCase;
import com.andinaseguros.customer.usecases.service.cliente.PublicarClientesExistentesUseCase;
import com.andinaseguros.customer.usecases.service.vehiculo.ListarVehiculosClienteUseCase;
import com.andinaseguros.customer.usecases.service.vehiculo.ObtenerVehiculoClienteUseCase;
import java.time.LocalDate;
import java.util.List;
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

/** Riesgo S2: las mismas reglas por rol y propietario que tenía el monolito para /api/clientes. */
@WebMvcTest(ClienteController.class)
@ContextConfiguration(classes = CustomerServiceApplication.class)
@Import({SecurityConfig.class, AccesoRecursos.class, GlobalExceptionHandler.class})
class ControlAccesoClientesTest {
    @Autowired MockMvc mvc;
    @MockBean RegistrarClienteUseCase crearCliente;
    @MockBean ListarClientesUseCase listarClientes;
    @MockBean ObtenerClienteUseCase obtenerCliente;
    @MockBean RegistrarVehiculoUseCase crearVehiculo;
    @MockBean ListarVehiculosClienteUseCase listarVehiculos;
    @MockBean ObtenerVehiculoClienteUseCase obtenerVehiculo;
    @MockBean ActualizarContactoClienteUseCase actualizarContacto;
    @MockBean PublicarClientesExistentesUseCase publicarClientesExistentes;
    @MockBean JwtDecoder jwtDecoder;

    private final UUID ana = UUID.randomUUID();
    private final UUID luis = UUID.randomUUID();

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
    }

    @Test
    void elClienteVeSuRegistroYSusVehiculos() throws Exception {
        when(obtenerCliente.execute(ana))
                .thenReturn(
                        new ClienteResponse(
                                ana, "DNI", "70000001", "Ana", "Torres", LocalDate.of(1990, 1, 15), null, null, true));
        when(listarVehiculos.execute(ana)).thenReturn(List.of());

        mvc.perform(get("/api/clientes/{id}", ana).with(cliente(ana))).andExpect(status().isOk());
        mvc.perform(get("/api/clientes/{id}/vehiculos", ana).with(cliente(ana))).andExpect(status().isOk());
    }

    @Test
    void elClienteNoVeAOtroClienteNiLaLista() throws Exception {
        mvc.perform(get("/api/clientes/{id}", luis).with(cliente(ana))).andExpect(status().isForbidden());
        mvc.perform(get("/api/clientes/{id}/vehiculos", luis).with(cliente(ana)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/clientes").with(cliente(ana))).andExpect(status().isForbidden());
    }

    @Test
    void elActuarioConsultaPeroNoCreaClientes() throws Exception {
        mvc.perform(get("/api/clientes").with(rol("ACTUARIO"))).andExpect(status().isOk());
        mvc.perform(
                        post("/api/clientes")
                                .with(rol("ACTUARIO"))
                                .contentType("application/json")
                                .content(
                                        "{\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"1\",\"nombres\":\"A\","
                                                + "\"apellidos\":\"B\",\"fechaNacimiento\":\"1990-01-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloElAdminEjecutaElBackfill() throws Exception {
        mvc.perform(post("/api/clientes/eventos/reenvio").with(rol("AGENTE"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/clientes/eventos/reenvio").with(rol("ADMIN"))).andExpect(status().isAccepted());
    }

    /** Fase 7: un id que no es UUID o un JSON roto es un error del cliente, no un 500. */
    @Test
    void unaSolicitudMalFormadaEs400YNo500() throws Exception {
        mvc.perform(get("/api/clientes/{id}", "no-es-uuid").with(rol("ADMIN"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/clientes").with(rol("ADMIN")).contentType("application/json").content("{"))
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
