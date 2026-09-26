package com.andinaseguros.usecases.service.cliente;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.andinaseguros.entities.enums.EstadoPoliza;
import com.andinaseguros.entities.enums.EstadoRenovacion;
import com.andinaseguros.entities.enums.RolUsuario;
import com.andinaseguros.entities.model.Cliente;
import com.andinaseguros.entities.model.Poliza;
import com.andinaseguros.entities.model.PropuestaRenovacion;
import com.andinaseguros.entities.model.Usuario;
import com.andinaseguros.entities.valueobject.Dinero;
import com.andinaseguros.entities.valueobject.PeriodoVigencia;
import com.andinaseguros.usecases.port.out.repository.ClienteRepository;
import com.andinaseguros.usecases.port.out.repository.PolizaRepository;
import com.andinaseguros.usecases.port.out.repository.RenovacionRepository;
import com.andinaseguros.usecases.port.out.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObtenerMiCuentaUseCaseTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final PolizaRepository polizas = mock(PolizaRepository.class);
    private final RenovacionRepository renovaciones = mock(RenovacionRepository.class);
    private final ObtenerMiCuentaUseCase useCase =
            new ObtenerMiCuentaUseCase(usuarios, clientes, polizas, renovaciones);

    @Test
    void devuelveRespuestaVaciaCuandoElCorreoNoTieneClienteVinculado() {
        var usuario =
                new Usuario(
                        UUID.randomUUID(),
                        "a@x.com",
                        "a@x.com",
                        null,
                        "sub-1",
                        RolUsuario.CLIENTE,
                        true,
                        null,
                        false);
        when(usuarios.buscarPorUsername("a@x.com")).thenReturn(Optional.of(usuario));
        when(clientes.buscarPorCorreo("a@x.com")).thenReturn(Optional.empty());

        var respuesta = useCase.execute("a@x.com");

        assertThat(respuesta.cliente()).isNull();
        assertThat(respuesta.polizas()).isEmpty();
        verifyNoInteractions(polizas, renovaciones);
    }

    @Test
    void devuelveLaPolizaDelClienteSinRenovaciones() {
        var clienteId = UUID.randomUUID();
        var usuario =
                new Usuario(
                        UUID.randomUUID(),
                        "a@x.com",
                        "a@x.com",
                        null,
                        "sub-1",
                        RolUsuario.CLIENTE,
                        true,
                        null,
                        false);
        var cliente =
                new Cliente(
                        clienteId,
                        "DNI",
                        "12345678",
                        "Ana",
                        "Ramírez",
                        LocalDate.now().minusYears(30),
                        "a@x.com",
                        "999999999",
                        true);
        var poliza =
                new Poliza(
                        UUID.randomUUID(),
                        "POL-001",
                        UUID.randomUUID(),
                        clienteId,
                        UUID.randomUUID(),
                        new Dinero(new BigDecimal("1200.00"), "PEN"),
                        new PeriodoVigencia(LocalDate.now(), LocalDate.now().plusDays(180)),
                        EstadoPoliza.VIGENTE);

        when(usuarios.buscarPorUsername("a@x.com")).thenReturn(Optional.of(usuario));
        when(clientes.buscarPorCorreo("a@x.com")).thenReturn(Optional.of(cliente));
        when(polizas.listarPorCliente(clienteId)).thenReturn(List.of(poliza));
        when(renovaciones.listarPorPoliza(poliza.getId())).thenReturn(List.of());

        var respuesta = useCase.execute("a@x.com");

        assertThat(respuesta.cliente().correo()).isEqualTo("a@x.com");
        assertThat(respuesta.polizas()).hasSize(1);
        assertThat(respuesta.polizas().get(0).poliza().numero()).isEqualTo("POL-001");
        assertThat(respuesta.polizas().get(0).renovaciones()).isEmpty();
    }

    @Test
    void incluyeLasRenovacionesDeCadaPoliza() {
        var clienteId = UUID.randomUUID();
        var usuario =
                new Usuario(
                        UUID.randomUUID(),
                        "a@x.com",
                        "a@x.com",
                        null,
                        "sub-1",
                        RolUsuario.CLIENTE,
                        true,
                        null,
                        false);
        var cliente =
                new Cliente(
                        clienteId,
                        "DNI",
                        "12345678",
                        "Ana",
                        "Ramírez",
                        LocalDate.now().minusYears(30),
                        "a@x.com",
                        "999999999",
                        true);
        var poliza =
                new Poliza(
                        UUID.randomUUID(),
                        "POL-001",
                        UUID.randomUUID(),
                        clienteId,
                        UUID.randomUUID(),
                        new Dinero(new BigDecimal("1200.00"), "PEN"),
                        new PeriodoVigencia(LocalDate.now(), LocalDate.now().plusDays(180)),
                        EstadoPoliza.VIGENTE);
        var renovacion =
                new PropuestaRenovacion(
                        UUID.randomUUID(),
                        poliza.getId(),
                        new Dinero(new BigDecimal("1200.00"), "PEN"),
                        new Dinero(new BigDecimal("1260.00"), "PEN"),
                        new BigDecimal("5.00"),
                        0,
                        EstadoRenovacion.PENDIENTE,
                        "Renovación sin siniestros",
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(30),
                        null,
                        null);

        when(usuarios.buscarPorUsername("a@x.com")).thenReturn(Optional.of(usuario));
        when(clientes.buscarPorCorreo("a@x.com")).thenReturn(Optional.of(cliente));
        when(polizas.listarPorCliente(clienteId)).thenReturn(List.of(poliza));
        when(renovaciones.listarPorPoliza(poliza.getId())).thenReturn(List.of(renovacion));

        var respuesta = useCase.execute("a@x.com");

        assertThat(respuesta.polizas().get(0).renovaciones()).hasSize(1);
        assertThat(respuesta.polizas().get(0).renovaciones().get(0).estado())
                .isEqualTo(EstadoRenovacion.PENDIENTE);
    }
}
