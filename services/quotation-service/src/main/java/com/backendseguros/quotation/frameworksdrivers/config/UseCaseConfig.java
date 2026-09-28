package com.backendseguros.quotation.frameworksdrivers.config;

import com.backendseguros.quotation.entities.service.MotorDeTarificacion;
import com.backendseguros.quotation.interfaceadapters.in.messaging.CustomerEventsListener;
import com.backendseguros.quotation.interfaceadapters.in.messaging.PolicyEventsListener;
import com.backendseguros.quotation.interfaceadapters.out.cliente.CustomerServiceDirectorioAdapter;
import com.backendseguros.quotation.interfaceadapters.out.id.UuidGeneratorAdapter;
import com.backendseguros.quotation.interfaceadapters.out.persistence.mongodb.adapter.MongoRechazoEmisionRepository;
import com.backendseguros.quotation.interfaceadapters.out.persistence.mongodb.adapter.MongoReferenciaClientesRepository;
import com.backendseguros.quotation.interfaceadapters.out.time.SystemClockAdapter;
import com.backendseguros.quotation.usecases.port.in.CrearCotizacionInputPort;
import com.backendseguros.quotation.usecases.port.out.cliente.DirectorioClientesPort;
import com.backendseguros.quotation.usecases.port.out.event.DomainEventPublisherPort;
import com.backendseguros.quotation.usecases.port.out.id.IdGeneratorPort;
import com.backendseguros.quotation.usecases.port.out.repository.CotizacionRepository;
import com.backendseguros.quotation.usecases.port.out.repository.ReferenciaClientesRepository;
import com.backendseguros.quotation.usecases.port.out.repository.TablaTarifariaRepository;
import com.backendseguros.quotation.usecases.port.out.time.ClockPort;
import com.backendseguros.quotation.usecases.port.out.transaccion.TransaccionPort;
import com.backendseguros.quotation.usecases.service.cotizacion.AceptarCotizacionUseCase;
import com.backendseguros.quotation.usecases.service.cotizacion.CrearCotizacionUseCase;
import com.backendseguros.quotation.usecases.service.cotizacion.ListarCotizacionesPendientesEmisionUseCase;
import com.backendseguros.quotation.usecases.service.cotizacion.ListarCotizacionesUseCase;
import com.backendseguros.quotation.usecases.service.cotizacion.MarcarCotizacionEmitidaUseCase;
import com.backendseguros.quotation.usecases.service.cotizacion.ObtenerCotizacionUseCase;
import com.backendseguros.quotation.usecases.service.cotizacion.RegistrarRechazoEmisionUseCase;
import com.backendseguros.quotation.usecases.service.referencia.ActualizarReferenciasUseCase;
import com.backendseguros.quotation.usecases.service.tarifa.CrearTablaTarifariaUseCase;
import com.backendseguros.quotation.usecases.service.tarifa.ListarTablasTarifariasUseCase;
import com.backendseguros.quotation.usecases.service.tarifa.ObtenerTablaTarifariaUseCase;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Arma los casos de uso (sin anotaciones de Spring) con sus adaptadores. */
@Configuration
public class UseCaseConfig {

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
    MotorDeTarificacion motorDeTarificacion() {
        return new MotorDeTarificacion();
    }

    @Bean
    ReferenciaClientesRepository referenciaClientesRepository(MongoTemplate mongoTemplate, Clock clock) {
        return new MongoReferenciaClientesRepository(mongoTemplate, clock);
    }

    /**
     * Paso 5.3: timeout de 2 s, un reintento y circuit breaker "customer" (application.yml).
     * HTTP/1.1 fijo: sobre http:// el cliente del JDK intenta subir a HTTP/2 (h2c).
     */
    @Bean
    DirectorioClientesPort directorioClientes(
            @Value("${app.customer-service.url}") String url,
            @Value("${app.customer-service.timeout:2s}") Duration timeout,
            CircuitBreakerRegistry circuitBreakers,
            RetryRegistry retries) {
        HttpClient httpClient =
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(timeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(timeout);
        return new CustomerServiceDirectorioAdapter(
                RestClient.builder().baseUrl(url).requestFactory(factory).build(),
                circuitBreakers.circuitBreaker("customer"),
                retries.retry("customer"));
    }

    @Bean
    CrearCotizacionInputPort crearCotizacion(
            ReferenciaClientesRepository referencias,
            DirectorioClientesPort directorio,
            TablaTarifariaRepository tablas,
            CotizacionRepository cotizaciones,
            MotorDeTarificacion motor) {
        return new CrearCotizacionUseCase(referencias, directorio, tablas, cotizaciones, motor);
    }

    @Bean
    AceptarCotizacionUseCase aceptarCotizacion(
            CotizacionRepository cotizaciones,
            DomainEventPublisherPort eventos,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        return new AceptarCotizacionUseCase(cotizaciones, eventos, transaccion, clock, ids);
    }

    @Bean
    ListarCotizacionesUseCase listarCotizaciones(CotizacionRepository cotizaciones) {
        return new ListarCotizacionesUseCase(cotizaciones);
    }

    @Bean
    ListarCotizacionesPendientesEmisionUseCase listarPendientesEmision(CotizacionRepository cotizaciones) {
        return new ListarCotizacionesPendientesEmisionUseCase(cotizaciones);
    }

    @Bean
    ObtenerCotizacionUseCase obtenerCotizacion(CotizacionRepository cotizaciones) {
        return new ObtenerCotizacionUseCase(cotizaciones);
    }

    @Bean
    MarcarCotizacionEmitidaUseCase marcarCotizacionEmitida(CotizacionRepository cotizaciones) {
        return new MarcarCotizacionEmitidaUseCase(cotizaciones);
    }

    @Bean
    CrearTablaTarifariaUseCase crearTablaTarifaria(TablaTarifariaRepository tablas) {
        return new CrearTablaTarifariaUseCase(tablas);
    }

    @Bean
    ListarTablasTarifariasUseCase listarTablasTarifarias(TablaTarifariaRepository tablas) {
        return new ListarTablasTarifariasUseCase(tablas);
    }

    @Bean
    ObtenerTablaTarifariaUseCase obtenerTablaTarifaria(TablaTarifariaRepository tablas) {
        return new ObtenerTablaTarifariaUseCase(tablas);
    }

    @Bean
    ActualizarReferenciasUseCase actualizarReferencias(ReferenciaClientesRepository referencias) {
        return new ActualizarReferenciasUseCase(referencias);
    }

    @Bean
    CustomerEventsListener customerEventsListener(ActualizarReferenciasUseCase actualizarReferencias) {
        return new CustomerEventsListener(actualizarReferencias);
    }

    @Bean
    RegistrarRechazoEmisionUseCase registrarRechazoEmision(
            CotizacionRepository cotizaciones, MongoTemplate mongoTemplate, Clock clock) {
        return new RegistrarRechazoEmisionUseCase(cotizaciones, new MongoRechazoEmisionRepository(mongoTemplate, clock));
    }

    @Bean
    PolicyEventsListener policyEventsListener(
            MarcarCotizacionEmitidaUseCase marcarEmitida, RegistrarRechazoEmisionUseCase registrarRechazo) {
        return new PolicyEventsListener(marcarEmitida, registrarRechazo);
    }
}
