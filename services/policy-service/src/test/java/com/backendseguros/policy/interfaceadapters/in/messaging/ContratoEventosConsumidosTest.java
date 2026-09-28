package com.backendseguros.policy.interfaceadapters.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import com.backendseguros.policy.contratos.Contratos;
import com.backendseguros.policy.usecases.service.referencia.ActualizarProyeccionesUseCase;
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
    private final ActualizarProyeccionesUseCase proyecciones = mock(ActualizarProyeccionesUseCase.class);
    private final ProyeccionesListener listener = new ProyeccionesListener(proyecciones);

    @ParameterizedTest
    @ValueSource(strings = {"quote.accepted.v1"})
    void aceptaLaCotizacionAceptada(String routingKey) throws Exception {
        listener.cotizacion(json.treeToValue(Contratos.ejemplo(routingKey), ProyeccionesListener.QuoteEvent.class), mensaje());
        llegoAlCasoDeUso(proyecciones);
    }

    @ParameterizedTest
    @ValueSource(strings = {"claim.registered.v1", "claim.status-changed.v1"})
    void aceptaLosEventosDeSiniestros(String routingKey) throws Exception {
        listener.siniestro(json.treeToValue(Contratos.ejemplo(routingKey), ProyeccionesListener.ClaimEvent.class), mensaje());
        llegoAlCasoDeUso(proyecciones);
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
