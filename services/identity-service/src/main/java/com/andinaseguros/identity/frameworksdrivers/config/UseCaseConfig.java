package com.andinaseguros.identity.frameworksdrivers.config;

import com.andinaseguros.identity.interfaceadapters.in.messaging.CustomerEventsListener;
import com.andinaseguros.identity.interfaceadapters.out.external.facebook.AesGcmSecretEncryptionAdapter;
import com.andinaseguros.identity.interfaceadapters.out.external.facebook.FacebookOAuthAdapter;
import com.andinaseguros.identity.interfaceadapters.out.external.facebook.FacebookProperties;
import com.andinaseguros.identity.interfaceadapters.out.id.UuidGeneratorAdapter;
import com.andinaseguros.identity.interfaceadapters.out.persistence.mongodb.adapter.MongoCorreoClienteRepository;
import com.andinaseguros.identity.interfaceadapters.out.security.BCryptPasswordEncoderAdapter;
import com.andinaseguros.identity.interfaceadapters.out.security.RsaJwtTokenAdapter;
import com.andinaseguros.identity.interfaceadapters.out.security.google.GoogleIdentityVerifierAdapter;
import com.andinaseguros.identity.interfaceadapters.out.security.mfa.TotpSecurityAdapter;
import com.andinaseguros.identity.interfaceadapters.out.security.mfa.ZxingQrCodeAdapter;
import com.andinaseguros.identity.interfaceadapters.out.security.redis.RedisLoginTicketAdapter;
import com.andinaseguros.identity.interfaceadapters.out.security.redis.RedisMfaChallengeAdapter;
import com.andinaseguros.identity.interfaceadapters.out.security.redis.RedisOAuthStateAdapter;
import com.andinaseguros.identity.interfaceadapters.out.security.redis.RedisRevocacionAdapter;
import com.andinaseguros.identity.interfaceadapters.out.time.SystemClockAdapter;
import com.andinaseguros.identity.usecases.port.out.facebook.FacebookOAuthPort;
import com.andinaseguros.identity.usecases.port.out.facebook.OAuthStatePort;
import com.andinaseguros.identity.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.identity.usecases.port.out.repository.CorreoClienteRepository;
import com.andinaseguros.identity.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.identity.usecases.port.out.security.*;
import com.andinaseguros.identity.usecases.port.out.time.ClockPort;
import com.andinaseguros.identity.usecases.service.auth.*;
import com.andinaseguros.identity.usecases.service.cliente.ActualizarIndiceCorreoClienteUseCase;
import com.andinaseguros.identity.usecases.service.mfa.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.RSAKey;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(FacebookProperties.class)
public class UseCaseConfig {

    // --- Puertos básicos -------------------------------------------------------------------

    @Bean
    ClockPort clockPort() {
        return new SystemClockAdapter();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
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
    TotpSecurityAdapter totpSecurityAdapter() {
        return new TotpSecurityAdapter();
    }

    @Bean
    QrCodeGeneratorPort qrCodeGeneratorPort() {
        return new ZxingQrCodeAdapter();
    }

    @Bean
    CorreoClienteRepository correoClienteRepository(MongoTemplate mongoTemplate, Clock clock) {
        return new MongoCorreoClienteRepository(mongoTemplate, clock);
    }

    // --- JWT RS256 -------------------------------------------------------------------------

    @Bean
    TokenGeneratorPort tokenGeneratorPort(
            JwtEncoder jwtEncoder,
            RSAKey identityRsaKey,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expiration-seconds:3600}") long expiration,
            ClockPort clock) {
        return new RsaJwtTokenAdapter(jwtEncoder, identityRsaKey.getKeyID(), issuer, expiration, clock);
    }

    @Bean
    EmisorDeTokens emisorDeTokens(TokenGeneratorPort tokens, CorreoClienteRepository correos) {
        return new EmisorDeTokens(tokens, correos);
    }

    // --- Estado efímero en Redis (paso 2.5) --------------------------------------------------

    @Bean
    MfaChallengePort mfaChallengePort(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${app.mfa.challenge-expiration-seconds:300}") long ttl) {
        return new RedisMfaChallengeAdapter(redis, objectMapper, ttl);
    }

    @Bean
    OAuthStatePort oauthStatePort(
            StringRedisTemplate redis, ObjectMapper objectMapper, FacebookProperties properties) {
        return new RedisOAuthStateAdapter(redis, objectMapper, properties.oauthStateTtlSeconds());
    }

    @Bean
    LoginTicketPort loginTicketPort(
            StringRedisTemplate redis, ObjectMapper objectMapper, FacebookProperties properties) {
        return new RedisLoginTicketAdapter(redis, objectMapper, properties.loginTicketTtlSeconds());
    }

    @Bean
    RevocacionPort revocacionPort(
            StringRedisTemplate redis,
            @Value("${app.jwt.expiration-seconds:3600}") long expiration,
            Clock clock) {
        return new RedisRevocacionAdapter(redis, Duration.ofSeconds(expiration), clock);
    }

    @Bean
    GestionarSesionesUseCase gestionarSesiones(UsuarioRepository usuarios, RevocacionPort revocaciones) {
        return new GestionarSesionesUseCase(usuarios, revocaciones);
    }

    // --- Proveedores externos con resiliencia (paso 2.8) --------------------------------------

    @Bean
    GoogleIdentityVerifierPort googleIdentityVerifierPort(
            @Value("${app.google.client-id}") String clientId,
            @Value("${app.google.issuer:https://accounts.google.com}") String issuer,
            @Value("${app.google.jwk-set-uri:" + GoogleIdentityVerifierAdapter.GOOGLE_JWK_SET_URI + "}")
                    String jwkSetUri,
            @Value("${app.google.timeout-seconds:3}") long timeoutSeconds,
            CircuitBreakerRegistry circuitBreakers,
            BulkheadRegistry bulkheads) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        // Caché de las claves públicas de Google: con ella, una caída breve de Google no impide
        // validar tokens firmados con claves ya conocidas.
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                        .restOperations(new RestTemplate(factory))
                        .cache(new ConcurrentMapCache("google-jwks"))
                        .build();
        return new GoogleIdentityVerifierAdapter(
                decoder,
                clientId,
                issuer,
                circuitBreakers.circuitBreaker("google"),
                bulkheads.bulkhead("google"));
    }

    @Bean
    FacebookOAuthPort facebookOAuthPort(
            FacebookProperties properties,
            @Value("${app.facebook.timeout-seconds:5}") long timeoutSeconds,
            CircuitBreakerRegistry circuitBreakers,
            BulkheadRegistry bulkheads) {
        HttpClient httpClient =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        // El Graph API de Facebook responde con "text/javascript" en vez de "application/json"
        // en algunos endpoints (comportamiento heredado de JSONP); sin este ajuste el
        // conversor Jackson por defecto rechaza el cuerpo aunque sea JSON válido.
        RestClient client =
                RestClient.builder()
                        .requestFactory(factory)
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
        return new FacebookOAuthAdapter(
                client, properties, circuitBreakers.circuitBreaker("facebook"), bulkheads.bulkhead("facebook"));
    }

    @Bean
    SecretEncryptionPort facebookSecretEncryptionPort(FacebookProperties properties) {
        return new AesGcmSecretEncryptionAdapter(properties.tokenEncryptionKey());
    }

    // --- Casos de uso ------------------------------------------------------------------------

    @Bean
    RegistrarUsuarioUseCase registrarUsuario(
            UsuarioRepository usuarios, PasswordEncoderPort passwordEncoder, IdGeneratorPort ids) {
        return new RegistrarUsuarioUseCase(usuarios, passwordEncoder, ids);
    }

    @Bean
    AutenticarUsuarioUseCase autenticarUsuario(
            UsuarioRepository usuarios,
            PasswordEncoderPort passwordEncoder,
            EmisorDeTokens emisor,
            MfaChallengePort challenges) {
        return new AutenticarUsuarioUseCase(usuarios, passwordEncoder, emisor, challenges);
    }

    @Bean
    VerificarMfaUseCase verificarMfa(
            UsuarioRepository usuarios,
            MfaChallengePort challenges,
            TotpVerifierPort totp,
            EmisorDeTokens emisor) {
        return new VerificarMfaUseCase(usuarios, challenges, totp, emisor);
    }

    @Bean
    AutenticarConGoogleUseCase autenticarConGoogle(
            UsuarioRepository usuarios,
            CorreoClienteRepository correos,
            GoogleIdentityVerifierPort verifier,
            EmisorDeTokens emisor,
            IdGeneratorPort ids) {
        return new AutenticarConGoogleUseCase(usuarios, correos, verifier, emisor, ids);
    }

    @Bean
    AutenticarConFacebookUseCase autenticarConFacebook(
            OAuthStatePort states,
            FacebookOAuthPort facebook,
            UsuarioRepository usuarios,
            IdGeneratorPort ids,
            SecretEncryptionPort encryption,
            EmisorDeTokens emisor) {
        return new AutenticarConFacebookUseCase(states, facebook, usuarios, ids, encryption, emisor);
    }

    @Bean
    DesvincularFacebookUseCase desvincularFacebook(UsuarioRepository usuarios) {
        return new DesvincularFacebookUseCase(usuarios);
    }

    @Bean
    ObtenerPerfilUseCase obtenerPerfil(UsuarioRepository usuarios) {
        return new ObtenerPerfilUseCase(usuarios);
    }

    @Bean
    ConfigurarMfaUseCase configurarMfa(
            UsuarioRepository usuarios, MfaSecretGeneratorPort secrets, QrCodeGeneratorPort qr) {
        return new ConfigurarMfaUseCase(usuarios, secrets, qr);
    }

    @Bean
    ActivarMfaUseCase activarMfa(UsuarioRepository usuarios, TotpVerifierPort totp) {
        return new ActivarMfaUseCase(usuarios, totp);
    }

    @Bean
    DesactivarMfaUseCase desactivarMfa(UsuarioRepository usuarios, TotpVerifierPort totp) {
        return new DesactivarMfaUseCase(usuarios, totp);
    }

    @Bean
    ObtenerEstadoMfaUseCase obtenerEstadoMfa(UsuarioRepository usuarios) {
        return new ObtenerEstadoMfaUseCase(usuarios);
    }

    @Bean
    ActualizarIndiceCorreoClienteUseCase actualizarIndiceCorreoCliente(CorreoClienteRepository correos) {
        return new ActualizarIndiceCorreoClienteUseCase(correos);
    }

    @Bean
    CustomerEventsListener customerEventsListener(ActualizarIndiceCorreoClienteUseCase useCase) {
        return new CustomerEventsListener(useCase);
    }
}
