package com.andinaseguros.policy.frameworksdrivers.config;

import com.andinaseguros.policy.entities.service.CalculadorPrimaRenovacion;
import com.andinaseguros.policy.entities.service.EvaluadorRenovacion;
import com.andinaseguros.policy.entities.service.PoliticaVariacionPrima;
import com.andinaseguros.policy.interfaceadapters.in.messaging.ProyeccionesListener;
import com.andinaseguros.policy.interfaceadapters.out.id.UuidGeneratorAdapter;
import com.andinaseguros.policy.interfaceadapters.out.persistence.mongodb.adapter.MongoProyeccionesRepository;
import com.andinaseguros.policy.interfaceadapters.out.time.SystemClockAdapter;
import com.andinaseguros.policy.usecases.port.out.event.DomainEventPublisherPort;
import com.andinaseguros.policy.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.policy.usecases.port.out.repository.PolizaRepository;
import com.andinaseguros.policy.usecases.port.out.repository.RenovacionRepository;
import com.andinaseguros.policy.usecases.port.out.time.ClockPort;
import com.andinaseguros.policy.usecases.port.out.transaccion.TransaccionPort;
import com.andinaseguros.policy.usecases.service.poliza.EmitirPolizaUseCase;
import com.andinaseguros.policy.usecases.service.poliza.ListarMisPolizasUseCase;
import com.andinaseguros.policy.usecases.service.poliza.ListarPolizasUseCase;
import com.andinaseguros.policy.usecases.service.poliza.ObtenerPolizaUseCase;
import com.andinaseguros.policy.usecases.service.referencia.ActualizarProyeccionesUseCase;
import com.andinaseguros.policy.usecases.service.renovacion.AprobarRenovacionUseCase;
import com.andinaseguros.policy.usecases.service.renovacion.EvaluarRenovacionUseCase;
import com.andinaseguros.policy.usecases.service.renovacion.GenerarPolizaRenovadaUseCase;
import com.andinaseguros.policy.usecases.service.renovacion.ListarHistorialRenovacionesUseCase;
import com.andinaseguros.policy.usecases.service.renovacion.ListarRenovacionesUseCase;
import com.andinaseguros.policy.usecases.service.renovacion.ObtenerRenovacionUseCase;
import com.andinaseguros.policy.usecases.service.renovacion.RechazarRenovacionUseCase;
import com.andinaseguros.policy.interfaceadapters.out.sincronizacion.SincronizacionSiniestrosAdapter;
import com.andinaseguros.policy.usecases.port.out.repository.SincronizacionSiniestrosPort;
import java.time.Clock;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.beans.factory.annotation.Value;
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

    /** Implementa accepted_quotes y claim_ref (dos puertos, una clase). */
    @Bean
    MongoProyeccionesRepository proyecciones(MongoTemplate mongoTemplate, Clock clock) {
        return new MongoProyeccionesRepository(mongoTemplate, clock);
    }

    @Bean
    EmitirPolizaUseCase emitirPoliza(
            MongoProyeccionesRepository proyecciones,
            PolizaRepository polizas,
            DomainEventPublisherPort eventos,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        return new EmitirPolizaUseCase(proyecciones, polizas, eventos, transaccion, clock, ids);
    }

    @Bean
    ListarPolizasUseCase listarPolizas(PolizaRepository polizas) {
        return new ListarPolizasUseCase(polizas);
    }

    @Bean
    ObtenerPolizaUseCase obtenerPoliza(PolizaRepository polizas) {
        return new ObtenerPolizaUseCase(polizas);
    }

    @Bean
    ListarMisPolizasUseCase listarMisPolizas(PolizaRepository polizas, RenovacionRepository renovaciones) {
        return new ListarMisPolizasUseCase(polizas, renovaciones);
    }

    /**
     * claim_ref está al día si tuvo la carga inicial y la cola de siniestros no tiene eventos
     * esperando (paso 6.5).
     */
    @Bean
    SincronizacionSiniestrosPort sincronizacionSiniestros(
            MongoProyeccionesRepository proyecciones,
            AmqpAdmin rabbitAdmin,
            @Value("${app.rabbitmq.claim.queue}") String colaSiniestros) {
        return new SincronizacionSiniestrosAdapter(
                proyecciones::cargaInicialHecha,
                () -> {
                    QueueInformation info = rabbitAdmin.getQueueInfo(colaSiniestros);
                    if (info == null) {
                        throw new IllegalStateException("La cola " + colaSiniestros + " no existe");
                    }
                    return info.getMessageCount();
                });
    }

    @Bean
    EvaluarRenovacionUseCase evaluarRenovacion(
            PolizaRepository polizas,
            MongoProyeccionesRepository proyecciones,
            SincronizacionSiniestrosPort sincronizacion,
            RenovacionRepository renovaciones) {
        return new EvaluarRenovacionUseCase(
                polizas,
                proyecciones,
                sincronizacion,
                renovaciones,
                new EvaluadorRenovacion(),
                new CalculadorPrimaRenovacion(),
                new PoliticaVariacionPrima());
    }

    @Bean
    AprobarRenovacionUseCase aprobarRenovacion(RenovacionRepository renovaciones) {
        return new AprobarRenovacionUseCase(renovaciones);
    }

    @Bean
    RechazarRenovacionUseCase rechazarRenovacion(RenovacionRepository renovaciones) {
        return new RechazarRenovacionUseCase(renovaciones);
    }

    @Bean
    GenerarPolizaRenovadaUseCase generarPolizaRenovada(
            RenovacionRepository renovaciones,
            PolizaRepository polizas,
            DomainEventPublisherPort eventos,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        return new GenerarPolizaRenovadaUseCase(renovaciones, polizas, eventos, transaccion, clock, ids);
    }

    @Bean
    ListarHistorialRenovacionesUseCase listarHistorialRenovaciones(
            PolizaRepository polizas, RenovacionRepository renovaciones) {
        return new ListarHistorialRenovacionesUseCase(polizas, renovaciones);
    }

    @Bean
    ListarRenovacionesUseCase listarRenovaciones(RenovacionRepository renovaciones) {
        return new ListarRenovacionesUseCase(renovaciones);
    }

    @Bean
    ObtenerRenovacionUseCase obtenerRenovacion(RenovacionRepository renovaciones) {
        return new ObtenerRenovacionUseCase(renovaciones);
    }

    @Bean
    ActualizarProyeccionesUseCase actualizarProyecciones(MongoProyeccionesRepository proyecciones) {
        return new ActualizarProyeccionesUseCase(proyecciones, proyecciones);
    }

    @Bean
    ProyeccionesListener proyeccionesListener(ActualizarProyeccionesUseCase actualizarProyecciones) {
        return new ProyeccionesListener(actualizarProyecciones);
    }
}
