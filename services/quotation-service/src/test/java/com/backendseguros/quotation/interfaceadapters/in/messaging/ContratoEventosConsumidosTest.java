package com.backendseguros.quotation.interfaceadapters.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import com.backendseguros.quotation.contratos.Contratos;
import com.backendseguros.quotation.usecases.service.cotizacion.MarcarCotizacionEmitidaUseCase;
import com.backendseguros.quotation.usecases.service.cotizacion.RegistrarRechazoEmisionUseCase;
import com.backendseguros.quotation.usecases.service.referencia.ActualizarReferenciasUseCase;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/**
 * Pruebas de contrato del lado consumidor (fase 7, paso 7.6): el listener acepta el ejemplo de cada
 * evento que consume (contracts/events/ejemplos, validado contra su esquema) y llega al caso de
 * uso. Si un productor cambia el contrato de forma incompatible, esta prueba falla en CI.
 */
class ContratoEventosConsumidosTest {
    /** Igual que el ObjectMapper de Spring Boot que usa el conversor de RabbitMQ. */
    private final ObjectMapper json =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final ActualizarReferenciasUseCase referencias = mock(ActualizarReferenciasUseCase.class);
    private final MarcarCotizacionEmitidaUseCase marcarEmitida = mock(MarcarCotizacionEmitidaUseCase.class);
    private final RegistrarRechazoEmisionUseCase registrarRechazo = mock(RegistrarRechazoEmisionUseCase.class);

    @ParameterizedTest
    @ValueSource(strings = {"customer.registered.v1", "customer.updated.v1", "vehicle.registered.v1"})
    void aceptaLosEventosDeClientesYVehiculos(String routingKey) throws Exception {
        var evento = json.treeToValue(Contratos.ejemplo(routingKey), CustomerEventsListener.EventMessage.class);
        new CustomerEventsListener(referencias).consume(evento, mensaje());
        llegoAlCasoDeUso(referencias);
    }

    @ParameterizedTest
    @ValueSource(strings = {"policy.issued.v1", "policy.issuance-rejected.v1"})
    void aceptaLosEventosDePolizas(String routingKey) throws Exception {
        var evento = json.treeToValue(Contratos.ejemplo(routingKey), PolicyEventsListener.PolicyEventMessage.class);
        new PolicyEventsListener(marcarEmitida, registrarRechazo).consume(evento, mensaje());
        llegoAlCasoDeUso(marcarEmitida, registrarRechazo);
    }

    private static Message mensaje() {
        return new Message(new byte[0], new MessageProperties());
    }

    private static void llegoAlCasoDeUso(Object... casosDeUso) {
        int llamadas = 0;
        for (Object casoDeUso : casosDeUso) {
            llamadas += mockingDetails(casoDeUso).getInvocations().size();
        }
        assertThat(llamadas).as("el evento debe llegar al caso de uso").isPositive();
    }
}
