package com.andinaseguros.frameworksdrivers.configuration.spring;

import com.andinaseguros.usecases.port.in.ConsultarInformacionVehiculoUseCase;
import com.andinaseguros.usecases.service.ConsultarInformacionVehiculoService;
import com.andinaseguros.usecases.service.auth.AutenticarConGoogleUseCase;
import com.andinaseguros.usecases.service.auth.AutenticarUsuarioUseCase;
import com.andinaseguros.usecases.service.auth.AutenticarConFacebookUseCase;
import com.andinaseguros.usecases.service.auth.DesvincularFacebookUseCase;
import com.andinaseguros.usecases.service.auth.RegistrarUsuarioUseCase;
import com.andinaseguros.usecases.service.auth.VerificarMfaUseCase;
import com.andinaseguros.usecases.service.auth.ObtenerPerfilUseCase;
import com.andinaseguros.usecases.service.mfa.*;
import com.andinaseguros.usecases.service.cliente.CrearClienteUseCase;
import com.andinaseguros.usecases.service.cliente.ListarClientesUseCase;
import com.andinaseguros.usecases.service.cliente.ObtenerClienteUseCase;
import com.andinaseguros.usecases.service.cliente.ObtenerMiCuentaUseCase;
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
import com.andinaseguros.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.usecases.port.out.repository.VehiculoRepository;
import com.andinaseguros.entities.service.CalculadorPrimaRenovacion;
import com.andinaseguros.entities.service.EvaluadorRenovacion;
import com.andinaseguros.entities.service.MotorDeTarificacion;
import com.andinaseguros.entities.service.PoliticaVariacionPrima;
import com.andinaseguros.interfaceadapters.out.event.*;
import com.andinaseguros.interfaceadapters.out.external.jsonpe.*;
import com.andinaseguros.interfaceadapters.out.external.facebook.*;
import com.andinaseguros.interfaceadapters.out.id.UuidGeneratorAdapter;
import com.andinaseguros.interfaceadapters.out.security.*;
import com.andinaseguros.interfaceadapters.out.security.google.GoogleIdentityVerifierAdapter;
import com.andinaseguros.interfaceadapters.out.security.mfa.*;
import com.andinaseguros.interfaceadapters.out.time.SystemClockAdapter;
import com.andinaseguros.usecases.port.out.event.DomainEventPublisherPort;
import com.andinaseguros.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.usecases.port.out.facebook.FacebookOAuthPort;
import com.andinaseguros.usecases.port.out.facebook.OAuthStatePort;
import com.andinaseguros.usecases.port.out.security.*;
import com.andinaseguros.usecases.port.out.time.ClockPort;
import com.andinaseguros.usecases.port.out.vehicle.VehicleInformationPort;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({JsonPeProperties.class, FacebookProperties.class, RabbitMqProperties.class})
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
    PasswordEncoderPort passwordEncoderPort() {
        return new BCryptPasswordEncoderAdapter();
    }

    @Bean
    TotpSecurityAdapter totpSecurityAdapter() { return new TotpSecurityAdapter(); }

    @Bean
    QrCodeGeneratorPort qrCodeGeneratorPort() { return new ZxingQrCodeAdapter(); }

    @Bean
    MfaChallengePort mfaChallengePort(ClockPort clock, @Value("${app.mfa.challenge-expiration-seconds:300}") long expiration) {
        return new InMemoryMfaChallengeAdapter(clock, expiration);
    }

    @Bean
    JwtTokenAdapter jwtTokenAdapter(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds:3600}") long expiration,
            ClockPort clock) {
        return new JwtTokenAdapter(secret, expiration, clock);
    }

    @Bean
    RegistrarUsuarioUseCase registrarUsuario(
            UsuarioRepository usuarios, PasswordEncoderPort passwordEncoder, IdGeneratorPort ids) {
        return new RegistrarUsuarioUseCase(usuarios, passwordEncoder, ids);
    }

    @Bean
    AutenticarUsuarioUseCase autenticarUsuario(
            UsuarioRepository usuarios,
            PasswordEncoderPort passwordEncoder,
            TokenGeneratorPort tokenGenerator,
            MfaChallengePort challenges) {
        return new AutenticarUsuarioUseCase(usuarios, passwordEncoder, tokenGenerator, challenges);
    }

    @Bean
    ConfigurarMfaUseCase configurarMfa(UsuarioRepository usuarios, MfaSecretGeneratorPort secrets, QrCodeGeneratorPort qr) {
        return new ConfigurarMfaUseCase(usuarios, secrets, qr);
    }

    @Bean
    ActivarMfaUseCase activarMfa(UsuarioRepository usuarios, TotpVerifierPort totp) { return new ActivarMfaUseCase(usuarios, totp); }

    @Bean
    DesactivarMfaUseCase desactivarMfa(UsuarioRepository usuarios, TotpVerifierPort totp) { return new DesactivarMfaUseCase(usuarios, totp); }

    @Bean
    ObtenerEstadoMfaUseCase obtenerEstadoMfa(UsuarioRepository usuarios) { return new ObtenerEstadoMfaUseCase(usuarios); }

    @Bean
    VerificarMfaUseCase verificarMfa(UsuarioRepository usuarios, MfaChallengePort challenges, TotpVerifierPort totp, TokenGeneratorPort tokens) {
        return new VerificarMfaUseCase(usuarios, challenges, totp, tokens);
    }

    @Bean
    ObtenerPerfilUseCase obtenerPerfil(UsuarioRepository usuarios) {
        return new ObtenerPerfilUseCase(usuarios);
    }

    @Bean
    GoogleIdentityVerifierPort googleIdentityVerifierPort(
            @Value("${app.google.client-id}") String clientId,
            @Value("${app.google.issuer:https://accounts.google.com}") String issuer) {
        JwtDecoder jwtDecoder =
                NimbusJwtDecoder.withJwkSetUri(GoogleIdentityVerifierAdapter.GOOGLE_JWK_SET_URI)
                        .build();
        return new GoogleIdentityVerifierAdapter(jwtDecoder, clientId, issuer);
    }

    @Bean
    AutenticarConGoogleUseCase autenticarConGoogle(
            UsuarioRepository usuarios,
            ClienteRepository clientes,
            GoogleIdentityVerifierPort googleIdentityVerifier,
            TokenGeneratorPort tokenGenerator,
            IdGeneratorPort ids) {
        return new AutenticarConGoogleUseCase(
                usuarios, clientes, googleIdentityVerifier, tokenGenerator, ids);
    }

    @Bean
    ObtenerMiCuentaUseCase obtenerMiCuenta(
            UsuarioRepository usuarios,
            ClienteRepository clientes,
            PolizaRepository polizas,
            RenovacionRepository renovaciones) {
        return new ObtenerMiCuentaUseCase(usuarios, clientes, polizas, renovaciones);
    }

    @Bean
    OAuthStatePort oauthStatePort(FacebookProperties properties) {
        return new InMemoryOAuthStateAdapter(properties.oauthStateTtlSeconds());
    }

    @Bean
    FacebookOAuthPort facebookOAuthPort(FacebookProperties properties) {
        // El Graph API de Facebook responde con "text/javascript" en vez de "application/json"
        // en algunos endpoints (comportamiento heredado de JSONP); sin este ajuste el
        // conversor Jackson por defecto rechaza el cuerpo aunque sea JSON válido.
        RestClient client =
                RestClient.builder()
                        .messageConverters(
                                converters ->
                                        converters.stream()
                                                .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                                                .map(MappingJackson2HttpMessageConverter.class::cast)
                                                .forEach(
                                                        converter -> {
                                                            List<MediaType> mediaTypes =
                                                                    new ArrayList<>(converter.getSupportedMediaTypes());
                                                            mediaTypes.add(MediaType.valueOf("text/javascript"));
                                                            converter.setSupportedMediaTypes(mediaTypes);
                                                        }))
                        .build();
        return new FacebookOAuthAdapter(client, properties);
    }

    @Bean
    SecretEncryptionPort facebookSecretEncryptionPort(FacebookProperties properties) {
        return new AesGcmSecretEncryptionAdapter(properties.tokenEncryptionKey());
    }

    @Bean
    LoginTicketPort loginTicketPort(FacebookProperties properties) {
        return new InMemoryLoginTicketAdapter(properties.loginTicketTtlSeconds());
    }

    @Bean
    AutenticarConFacebookUseCase autenticarConFacebook(
            OAuthStatePort states,
            FacebookOAuthPort facebook,
            UsuarioRepository usuarios,
            IdGeneratorPort ids,
            SecretEncryptionPort encryption,
            TokenGeneratorPort tokenGenerator) {
        return new AutenticarConFacebookUseCase(states, facebook, usuarios, ids, encryption, tokenGenerator);
    }

    @Bean
    DesvincularFacebookUseCase desvincularFacebook(UsuarioRepository usuarios) {
        return new DesvincularFacebookUseCase(usuarios);
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
    CrearClienteUseCase crearCliente(ClienteRepository clienteRepository) {
        return new CrearClienteUseCase(clienteRepository);
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
            ClockPort clock,
            IdGeneratorPort ids) {
        return new EmitirPolizaUseCase(
                cotizacionRepository, polizaRepository, eventPublisher, clock, ids);
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
    PolicyIssuedMessageMapper policyIssuedMessageMapper() {
        return new PolicyIssuedMessageMapper();
    }

    @Bean
    DomainEventPublisherPort domainEventPublisher(
            RabbitTemplate rabbitTemplate,
            PolicyIssuedMessageMapper mapper,
            RabbitMqProperties properties) {
        return new RabbitMqDomainEventPublisherAdapter(rabbitTemplate, mapper, properties);
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

