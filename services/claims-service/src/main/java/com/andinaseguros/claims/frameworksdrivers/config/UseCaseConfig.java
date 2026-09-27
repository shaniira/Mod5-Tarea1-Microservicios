package com.andinaseguros.claims.frameworksdrivers.config;

import com.andinaseguros.claims.interfaceadapters.in.messaging.PolicyEventsListener;
import com.andinaseguros.claims.interfaceadapters.out.id.UuidGeneratorAdapter;
import com.andinaseguros.claims.interfaceadapters.out.persistence.mongodb.adapter.MongoPolizaRefRepository;
import com.andinaseguros.claims.interfaceadapters.out.time.SystemClockAdapter;
import com.andinaseguros.claims.usecases.port.out.event.DomainEventPublisherPort;
import com.andinaseguros.claims.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.claims.usecases.port.out.repository.PolizaRefRepository;
import com.andinaseguros.claims.usecases.port.out.repository.SiniestroRepository;
import com.andinaseguros.claims.usecases.port.out.time.ClockPort;
import com.andinaseguros.claims.usecases.port.out.transaccion.TransaccionPort;
import com.andinaseguros.claims.usecases.service.poliza.ActualizarEstadoPolizaUseCase;
import com.andinaseguros.claims.usecases.service.poliza.RegistrarPolizaEmitidaUseCase;
import com.andinaseguros.claims.usecases.service.siniestro.ActualizarEstadoSiniestroUseCase;
import com.andinaseguros.claims.usecases.service.siniestro.ListarSiniestrosUseCase;
import com.andinaseguros.claims.usecases.service.siniestro.PublicarSiniestrosExistentesUseCase;
import com.andinaseguros.claims.usecases.service.siniestro.RegistrarSiniestroUseCase;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

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
    PolizaRefRepository polizaRefRepository(MongoTemplate mongoTemplate, Clock clock) {
        return new MongoPolizaRefRepository(mongoTemplate, clock);
    }

    @Bean
    RegistrarSiniestroUseCase registrarSiniestro(
            PolizaRefRepository polizas,
            SiniestroRepository siniestros,
            DomainEventPublisherPort eventos,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        return new RegistrarSiniestroUseCase(polizas, siniestros, eventos, transaccion, clock, ids);
    }

    @Bean
    ActualizarEstadoSiniestroUseCase actualizarEstadoSiniestro(
            PolizaRefRepository polizas,
            SiniestroRepository siniestros,
            DomainEventPublisherPort eventos,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        return new ActualizarEstadoSiniestroUseCase(polizas, siniestros, eventos, transaccion, clock, ids);
    }

    @Bean
    ListarSiniestrosUseCase listarSiniestros(SiniestroRepository siniestros) {
        return new ListarSiniestrosUseCase(siniestros);
    }

    @Bean
    PublicarSiniestrosExistentesUseCase publicarSiniestrosExistentes(
            SiniestroRepository siniestros,
            PolizaRefRepository polizas,
            DomainEventPublisherPort eventos,
            ClockPort clock,
            IdGeneratorPort ids) {
        return new PublicarSiniestrosExistentesUseCase(siniestros, polizas, eventos, clock, ids);
    }

    @Bean
    RegistrarPolizaEmitidaUseCase registrarPolizaEmitida(PolizaRefRepository polizas) {
        return new RegistrarPolizaEmitidaUseCase(polizas);
    }

    @Bean
    ActualizarEstadoPolizaUseCase actualizarEstadoPoliza(PolizaRefRepository polizas) {
        return new ActualizarEstadoPolizaUseCase(polizas);
    }

    @Bean
    PolicyEventsListener policyEventsListener(
            RegistrarPolizaEmitidaUseCase registrarPolizaEmitida, ActualizarEstadoPolizaUseCase actualizarEstadoPoliza) {
        return new PolicyEventsListener(registrarPolizaEmitida, actualizarEstadoPoliza);
    }
}
