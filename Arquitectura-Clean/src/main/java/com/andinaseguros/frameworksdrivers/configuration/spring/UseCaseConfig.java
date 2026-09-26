package com.andinaseguros.frameworksdrivers.configuration.spring;

import com.andinaseguros.usecases.port.in.ConsultarInformacionVehiculoUseCase;
import com.andinaseguros.usecases.service.ConsultarInformacionVehiculoService;
import com.andinaseguros.usecases.service.cliente.ActualizarContactoClienteUseCase;
import com.andinaseguros.usecases.service.cliente.CrearClienteUseCase;
import com.andinaseguros.usecases.service.cliente.ListarClientesUseCase;
import com.andinaseguros.usecases.service.cliente.ObtenerClienteUseCase;
import com.andinaseguros.usecases.service.cliente.ObtenerMiCuentaUseCase;
import com.andinaseguros.usecases.service.cliente.PublicarClientesExistentesUseCase;
import com.andinaseguros.usecases.service.cotizacion.AceptarCotizacionUseCase;
import com.andinaseguros.usecases.service.cotizacion.CrearCotizacionUseCase;
import com.andinaseguros.usecases.service.cotizacion.ListarCotizacionesPendientesEmisionUseCase;
import com.andinaseguros.usecases.service.cotizacion.ListarCotizacionesUseCase;
import com.andinaseguros.usecases.service.cotizacion.ObtenerCotizacionUseCase;
import com.andinaseguros.usecases.service.poliza.EmitirPolizaUseCase;
import com.andinaseguros.usecases.service.poliza.ListarPolizasUseCase;
import com.andinaseguros.usecases.service.poliza.ObtenerPolizaUseCase;
import com.andinaseguros.usecases.service.renovacion.AprobarRenovacionUseCase;
import com.andinaseguros.usecases.service.renovacion.EvaluarRenovacionUseCase;
import com.andinaseguros.usecases.service.renovacion.GenerarPolizaRenovadaUseCase;
import com.andinaseguros.usecases.service.renovacion.ListarHistorialRenovacionesUseCase;
import com.andinaseguros.usecases.service.renovacion.ListarRenovacionesUseCase;
import com.andinaseguros.usecases.service.renovacion.ObtenerRenovacionUseCase;
import com.andinaseguros.usecases.service.renovacion.RechazarRenovacionUseCase;
import com.andinaseguros.usecases.service.siniestro.ActualizarEstadoSiniestroUseCase;
import com.andinaseguros.usecases.service.siniestro.ListarSiniestrosUseCase;
import com.andinaseguros.usecases.service.siniestro.RegistrarSiniestroUseCase;
import com.andinaseguros.usecases.service.tarifa.CrearTablaTarifariaUseCase;
import com.andinaseguros.usecases.service.tarifa.ListarTablasTarifariasUseCase;
import com.andinaseguros.usecases.service.tarifa.ObtenerTablaTarifariaUseCase;
import com.andinaseguros.usecases.service.vehiculo.CrearVehiculoUseCase;
import com.andinaseguros.usecases.service.vehiculo.ListarVehiculosClienteUseCase;
import com.andinaseguros.usecases.port.out.repository.ClienteRepository;
import com.andinaseguros.usecases.port.out.repository.CotizacionRepository;
import com.andinaseguros.usecases.port.out.repository.PolizaRepository;
import com.andinaseguros.usecases.port.out.repository.RenovacionRepository;
import com.andinaseguros.usecases.port.out.repository.SiniestroRepository;
import com.andinaseguros.usecases.port.out.repository.TablaTarifariaRepository;
import com.andinaseguros.usecases.port.out.repository.VehiculoRepository;
import com.andinaseguros.entities.service.CalculadorPrimaRenovacion;
import com.andinaseguros.entities.service.EvaluadorRenovacion;
import com.andinaseguros.entities.service.MotorDeTarificacion;
import com.andinaseguros.entities.service.PoliticaVariacionPrima;
import com.andinaseguros.interfaceadapters.out.event.*;
import com.andinaseguros.interfaceadapters.out.external.jsonpe.*;
import com.andinaseguros.interfaceadapters.out.id.UuidGeneratorAdapter;
import com.andinaseguros.interfaceadapters.out.time.SystemClockAdapter;
import com.andinaseguros.usecases.port.out.event.DomainEventPublisherPort;
import com.andinaseguros.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.usecases.port.out.time.ClockPort;
import com.andinaseguros.usecases.port.out.transaccion.TransaccionPort;
import com.andinaseguros.usecases.port.out.vehicle.VehicleInformationPort;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({JsonPeProperties.class, RabbitMqProperties.class})
public class UseCaseConfig {

    @Bean
    ClockPort clockPort() {
        return new SystemClockAdapter();
    }

    @Bean
    IdGeneratorPort idGeneratorPort() {
        return new UuidGeneratorAdapter();
    }

    @Bean
    ObtenerMiCuentaUseCase obtenerMiCuenta(
            ClienteRepository clientes, PolizaRepository polizas, RenovacionRepository renovaciones) {
        return new ObtenerMiCuentaUseCase(clientes, polizas, renovaciones);
    }

    @Bean
    MotorDeTarificacion motorDeTarificacion() {
        return new MotorDeTarificacion();
    }

    @Bean
    EvaluadorRenovacion evaluadorRenovacion() {
        return new EvaluadorRenovacion();
    }

    @Bean
    CalculadorPrimaRenovacion calculadorPrimaRenovacion() {
        return new CalculadorPrimaRenovacion();
    }

    @Bean
    PoliticaVariacionPrima politicaVariacionPrima() {
        return new PoliticaVariacionPrima();
    }

    @Bean
    CrearClienteUseCase crearCliente(
            ClienteRepository clienteRepository,
            DomainEventPublisherPort eventPublisher,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        return new CrearClienteUseCase(clienteRepository, eventPublisher, transaccion, clock, ids);
    }

    @Bean
    ActualizarContactoClienteUseCase actualizarContactoCliente(
            ClienteRepository clienteRepository,
            DomainEventPublisherPort eventPublisher,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        return new ActualizarContactoClienteUseCase(
                clienteRepository, eventPublisher, transaccion, clock, ids);
    }

    @Bean
    PublicarClientesExistentesUseCase publicarClientesExistentes(
            ClienteRepository clienteRepository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            IdGeneratorPort ids) {
        return new PublicarClientesExistentesUseCase(clienteRepository, eventPublisher, clock, ids);
    }

    @Bean
    ListarClientesUseCase listarClientes(ClienteRepository clienteRepository) {
        return new ListarClientesUseCase(clienteRepository);
    }

    @Bean
    ObtenerClienteUseCase obtenerCliente(ClienteRepository clienteRepository) {
        return new ObtenerClienteUseCase(clienteRepository);
    }

    @Bean
    CrearVehiculoUseCase crearVehiculo(
            ClienteRepository clienteRepository, VehiculoRepository vehiculoRepository) {
        return new CrearVehiculoUseCase(clienteRepository, vehiculoRepository);
    }

    @Bean
    ListarVehiculosClienteUseCase listarVehiculos(VehiculoRepository vehiculoRepository) {
        return new ListarVehiculosClienteUseCase(vehiculoRepository);
    }

    @Bean
    CrearTablaTarifariaUseCase crearTabla(TablaTarifariaRepository tablaTarifariaRepository) {
        return new CrearTablaTarifariaUseCase(tablaTarifariaRepository);
    }

    @Bean
    ListarTablasTarifariasUseCase listarTablas(TablaTarifariaRepository tablaTarifariaRepository) {
        return new ListarTablasTarifariasUseCase(tablaTarifariaRepository);
    }

    @Bean
    ObtenerTablaTarifariaUseCase obtenerTabla(TablaTarifariaRepository tablaTarifariaRepository) {
        return new ObtenerTablaTarifariaUseCase(tablaTarifariaRepository);
    }

    @Bean
    CrearCotizacionUseCase crearCotizacion(
            ClienteRepository clienteRepository,
            VehiculoRepository vehiculoRepository,
            TablaTarifariaRepository tablaTarifariaRepository,
            CotizacionRepository cotizacionRepository,
            MotorDeTarificacion motorDeTarificacion) {
        return new CrearCotizacionUseCase(
                clienteRepository,
                vehiculoRepository,
                tablaTarifariaRepository,
                cotizacionRepository,
                motorDeTarificacion);
    }

    @Bean
    ObtenerCotizacionUseCase obtenerCotizacion(CotizacionRepository cotizacionRepository) {
        return new ObtenerCotizacionUseCase(cotizacionRepository);
    }

    @Bean
    AceptarCotizacionUseCase aceptarCotizacion(CotizacionRepository cotizacionRepository) {
        return new AceptarCotizacionUseCase(cotizacionRepository);
    }

    @Bean
    ListarCotizacionesUseCase listarCotizaciones(CotizacionRepository cotizacionRepository) {
        return new ListarCotizacionesUseCase(cotizacionRepository);
    }

    @Bean
    ListarCotizacionesPendientesEmisionUseCase listarCotizacionesPendientes(
            CotizacionRepository cotizacionRepository, PolizaRepository polizaRepository) {
        return new ListarCotizacionesPendientesEmisionUseCase(
                cotizacionRepository, polizaRepository);
    }

    @Bean
    EmitirPolizaUseCase emitirPoliza(
            CotizacionRepository cotizacionRepository,
            PolizaRepository polizaRepository,
            DomainEventPublisherPort eventPublisher,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        return new EmitirPolizaUseCase(
                cotizacionRepository, polizaRepository, eventPublisher, transaccion, clock, ids);
    }

    @Bean
    ObtenerPolizaUseCase obtenerPoliza(PolizaRepository polizaRepository) {
        return new ObtenerPolizaUseCase(polizaRepository);
    }

    @Bean
    ListarPolizasUseCase listarPolizas(PolizaRepository polizaRepository) {
        return new ListarPolizasUseCase(polizaRepository);
    }

    @Bean
    VehicleInformationPort jsonPeVehicleInformation(JsonPeProperties properties) {
        RestClient client = timedRestClient(properties.baseUrl(), properties.timeoutSeconds());
        return new JsonPeVehicleInformationAdapter(
                new JsonPeClient(client, properties), new JsonPeVehicleMapper());
    }

    private RestClient timedRestClient(String baseUrl, Integer configuredTimeout) {
        long timeout = configuredTimeout == null ? 5 : configuredTimeout;
        HttpClient httpClient =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeout)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(timeout));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Bean
    ConsultarInformacionVehiculoUseCase consultarInformacionVehiculo(
            VehicleInformationPort vehicleInformationPort) {
        return new ConsultarInformacionVehiculoService(vehicleInformationPort);
    }

    @Bean
    RegistrarSiniestroUseCase registrarSiniestro(
            PolizaRepository polizaRepository, SiniestroRepository siniestroRepository) {
        return new RegistrarSiniestroUseCase(polizaRepository, siniestroRepository);
    }

    @Bean
    ActualizarEstadoSiniestroUseCase actualizarSiniestro(
            PolizaRepository polizaRepository, SiniestroRepository siniestroRepository) {
        return new ActualizarEstadoSiniestroUseCase(polizaRepository, siniestroRepository);
    }

    @Bean
    ListarSiniestrosUseCase listarSiniestros(SiniestroRepository siniestroRepository) {
        return new ListarSiniestrosUseCase(siniestroRepository);
    }

    @Bean
    EvaluarRenovacionUseCase evaluarRenovacion(
            PolizaRepository polizaRepository,
            SiniestroRepository siniestroRepository,
            RenovacionRepository renovacionRepository,
            EvaluadorRenovacion evaluadorRenovacion,
            CalculadorPrimaRenovacion calculadorPrimaRenovacion,
            PoliticaVariacionPrima politicaVariacionPrima) {
        return new EvaluarRenovacionUseCase(
                polizaRepository,
                siniestroRepository,
                renovacionRepository,
                evaluadorRenovacion,
                calculadorPrimaRenovacion,
                politicaVariacionPrima);
    }

    @Bean
    ObtenerRenovacionUseCase obtenerRenovacion(RenovacionRepository renovacionRepository) {
        return new ObtenerRenovacionUseCase(renovacionRepository);
    }

    @Bean
    AprobarRenovacionUseCase aprobarRenovacion(RenovacionRepository renovacionRepository) {
        return new AprobarRenovacionUseCase(renovacionRepository);
    }

    @Bean
    RechazarRenovacionUseCase rechazarRenovacion(RenovacionRepository renovacionRepository) {
        return new RechazarRenovacionUseCase(renovacionRepository);
    }

    @Bean
    GenerarPolizaRenovadaUseCase generarRenovada(
            RenovacionRepository renovacionRepository, PolizaRepository polizaRepository) {
        return new GenerarPolizaRenovadaUseCase(renovacionRepository, polizaRepository);
    }

    @Bean
    ListarRenovacionesUseCase listarRenovaciones(RenovacionRepository renovacionRepository) {
        return new ListarRenovacionesUseCase(renovacionRepository);
    }

    @Bean
    ListarHistorialRenovacionesUseCase historial(
            PolizaRepository polizaRepository, RenovacionRepository renovacionRepository) {
        return new ListarHistorialRenovacionesUseCase(polizaRepository, renovacionRepository);
    }
}

