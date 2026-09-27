package com.andinaseguros.customer.interfaceadapters.out.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.andinaseguros.customer.entities.enums.TipoUso;
import com.andinaseguros.customer.entities.enums.TipoVehiculo;
import com.andinaseguros.customer.entities.event.ClienteActualizadoEvent;
import com.andinaseguros.customer.entities.event.VehiculoRegistradoEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** El mensaje debe cumplir contracts/events: los consumidores de las fases 1 y 2 no cambian. */
class IntegrationEventMapperTest {
    private final IntegrationEventMapper mapper =
            new IntegrationEventMapper(new RabbitMqProperties("andina.events"));
    private final ObjectMapper json =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void customerUpdatedConservaLosCamposDelBackendYAgregaBirthDate() throws Exception {
        UUID clienteId = UUID.randomUUID();
        var outbound =
                mapper.map(
                        new ClienteActualizadoEvent(
                                UUID.randomUUID(),
                                Instant.parse("2026-09-27T10:00:00Z"),
                                clienteId,
                                "DNI",
                                "70000001",
                                "Ana",
                                "Torres",
                                LocalDate.of(1990, 1, 15),
                                "ana@andina.local",
                                "921175206",
                                true,
                                7),
                        "corr-1");

        assertThat(outbound.exchange()).isEqualTo("andina.events");
        assertThat(outbound.routingKey()).isEqualTo("customer.updated.v1");
        JsonNode node = json.readTree(json.writeValueAsString(outbound.message()));
        assertThat(node.get("eventType").asText()).isEqualTo("CustomerUpdated");
        assertThat(node.get("aggregateId").asText()).isEqualTo(clienteId.toString());
        assertThat(node.get("aggregateVersion").asLong()).isEqualTo(7);
        assertThat(node.get("producer").asText()).isEqualTo("customer-service");
        assertThat(node.get("correlationId").asText()).isEqualTo("corr-1");
        assertThat(node.at("/data/customerId").asText()).isEqualTo(clienteId.toString());
        assertThat(node.at("/data/fullName").asText()).isEqualTo("Ana Torres");
        assertThat(node.at("/data/email").asText()).isEqualTo("ana@andina.local");
        assertThat(node.at("/data/phone").asText()).isEqualTo("921175206");
        assertThat(node.at("/data/active").asBoolean()).isTrue();
        assertThat(node.at("/data/birthDate").asText()).isEqualTo("1990-01-15");
    }

    @Test
    void vehicleRegisteredLlevaLoQueNecesitaLaTarificacion() throws Exception {
        UUID vehiculoId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        var outbound =
                mapper.map(
                        new VehiculoRegistradoEvent(
                                UUID.randomUUID(),
                                Instant.parse("2026-09-27T10:00:00Z"),
                                vehiculoId,
                                clienteId,
                                "ABC-123",
                                "Toyota",
                                "Yaris",
                                2022,
                                TipoVehiculo.AUTO,
                                TipoUso.TAXI,
                                "LIMA"),
                        null);

        assertThat(outbound.exchange()).isEqualTo("andina.events");
        assertThat(outbound.routingKey()).isEqualTo("vehicle.registered.v1");
        JsonNode node = json.readTree(json.writeValueAsString(outbound.message()));
        assertThat(node.get("eventType").asText()).isEqualTo("VehicleRegistered");
        assertThat(node.get("aggregateId").asText()).isEqualTo(vehiculoId.toString());
        assertThat(node.get("aggregateVersion").asLong()).isEqualTo(1);
        assertThat(node.at("/data/customerId").asText()).isEqualTo(clienteId.toString());
        assertThat(node.at("/data/plate").asText()).isEqualTo("ABC-123");
        assertThat(node.at("/data/manufactureYear").asInt()).isEqualTo(2022);
        assertThat(node.at("/data/vehicleType").asText()).isEqualTo("AUTO");
        assertThat(node.at("/data/usage").asText()).isEqualTo("TAXI");
    }
}
