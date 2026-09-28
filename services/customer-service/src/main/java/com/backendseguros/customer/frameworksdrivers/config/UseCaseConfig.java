package com.backendseguros.customer.frameworksdrivers.config;

import com.backendseguros.customer.interfaceadapters.out.cache.RedisVehicleInformationCacheAdapter;
import com.backendseguros.customer.interfaceadapters.out.external.jsonpe.JsonPeClient;
import com.backendseguros.customer.interfaceadapters.out.external.jsonpe.JsonPeProperties;
import com.backendseguros.customer.interfaceadapters.out.external.jsonpe.JsonPeVehicleInformationAdapter;
import com.backendseguros.customer.interfaceadapters.out.external.jsonpe.JsonPeVehicleMapper;
import com.backendseguros.customer.interfaceadapters.out.id.UuidGeneratorAdapter;
import com.backendseguros.customer.interfaceadapters.out.time.SystemClockAdapter;
import com.backendseguros.customer.usecases.port.in.ConsultarInformacionVehiculoUseCase;
import com.backendseguros.customer.usecases.port.in.RegistrarClienteUseCase;
import com.backendseguros.customer.usecases.port.in.RegistrarVehiculoUseCase;
import com.backendseguros.customer.usecases.port.out.event.DomainEventPublisherPort;
import com.backendseguros.customer.usecases.port.out.id.IdGeneratorPort;
import com.backendseguros.customer.usecases.port.out.repository.ClienteRepository;
import com.backendseguros.customer.usecases.port.out.repository.VehiculoRepository;
import com.backendseguros.customer.usecases.port.out.time.ClockPort;
import com.backendseguros.customer.usecases.port.out.transaccion.TransaccionPort;
import com.backendseguros.customer.usecases.port.out.vehicle.VehicleInformationCachePort;
import com.backendseguros.customer.usecases.port.out.vehicle.VehicleInformationPort;
import com.backendseguros.customer.usecases.service.cliente.ActualizarContactoClienteUseCase;
import com.backendseguros.customer.usecases.service.cliente.CrearClienteUseCase;
import com.backendseguros.customer.usecases.service.cliente.ListarClientesUseCase;
import com.backendseguros.customer.usecases.service.cliente.ObtenerClienteUseCase;
import com.backendseguros.customer.usecases.service.cliente.PublicarClientesExistentesUseCase;
import com.backendseguros.customer.usecases.service.placa.ConsultarInformacionVehiculoService;
import com.backendseguros.customer.usecases.service.vehiculo.CrearVehiculoUseCase;
import com.backendseguros.customer.usecases.service.vehiculo.ListarVehiculosClienteUseCase;
import com.backendseguros.customer.usecases.service.vehiculo.ObtenerVehiculoClienteUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Arma los casos de uso (sin anotaciones de Spring) con sus adaptadores. */
@Configuration
@EnableConfigurationProperties(JsonPeProperties.class)
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
    RegistrarClienteUseCase crearCliente(
            ClienteRepository clientes,
            DomainEventPublisherPort eventos,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        return new CrearClienteUseCase(clientes, eventos, transaccion, clock, ids);
    }

    @Bean
    ActualizarContactoClienteUseCase actualizarContactoCliente(
            ClienteRepository clientes,
            DomainEventPublisherPort eventos,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        return new ActualizarContactoClienteUseCase(clientes, eventos, transaccion, clock, ids);
    }

    @Bean
    ListarClientesUseCase listarClientes(ClienteRepository clientes) {
        return new ListarClientesUseCase(clientes);
    }

    @Bean
    ObtenerClienteUseCase obtenerCliente(ClienteRepository clientes) {
        return new ObtenerClienteUseCase(clientes);
    }

    @Bean
    PublicarClientesExistentesUseCase publicarClientesExistentes(
            ClienteRepository clientes,
            VehiculoRepository vehiculos,
            DomainEventPublisherPort eventos,
            ClockPort clock,
            IdGeneratorPort ids) {
        return new PublicarClientesExistentesUseCase(clientes, vehiculos, eventos, clock, ids);
    }

    @Bean
    RegistrarVehiculoUseCase crearVehiculo(
            ClienteRepository clientes,
            VehiculoRepository vehiculos,
            DomainEventPublisherPort eventos,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        return new CrearVehiculoUseCase(clientes, vehiculos, eventos, transaccion, clock, ids);
    }

    @Bean
    ListarVehiculosClienteUseCase listarVehiculosCliente(VehiculoRepository vehiculos) {
        return new ListarVehiculosClienteUseCase(vehiculos);
    }

    @Bean
    ObtenerVehiculoClienteUseCase obtenerVehiculoCliente(VehiculoRepository vehiculos) {
        return new ObtenerVehiculoClienteUseCase(vehiculos);
    }

    /**
     * Paso 3.2: timeout de 5 s en el RestClient; circuit breaker, reintento y bulkhead "jsonpe"
     * configurados en application.yml (resilience4j.*.instances.jsonpe).
     */
    @Bean
    VehicleInformationPort jsonPeVehicleInformation(
            JsonPeProperties properties,
            CircuitBreakerRegistry circuitBreakers,
            RetryRegistry retries,
            BulkheadRegistry bulkheads) {
        long timeout = properties.timeoutSeconds() == null ? 5 : properties.timeoutSeconds();
        // HTTP/1.1 fijo: sobre http:// (simulador, proxies internos) el cliente del JDK intenta
        // subir a HTTP/2 con "Upgrade: h2c" y la respuesta se pierde. Con https no cambia nada.
        HttpClient httpClient =
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(timeout))
                        .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(timeout));
        RestClient client =
                RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory).build();
        return new JsonPeVehicleInformationAdapter(
                new JsonPeClient(client, properties),
                new JsonPeVehicleMapper(),
                circuitBreakers.circuitBreaker("jsonpe"),
                retries.retry("jsonpe"),
                bulkheads.bulkhead("jsonpe"));
    }

    @Bean
    VehicleInformationCachePort vehicleInformationCache(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${app.placas.cache-ttl:24h}") Duration ttl) {
        return new RedisVehicleInformationCacheAdapter(redis, objectMapper, ttl);
    }

    @Bean
    ConsultarInformacionVehiculoUseCase consultarInformacionVehiculo(
            VehicleInformationPort proveedor, VehicleInformationCachePort cache) {
        return new ConsultarInformacionVehiculoService(proveedor, cache);
    }
}
