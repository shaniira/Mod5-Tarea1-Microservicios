package com.andinaseguros.interfaceadapters.out.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.andinaseguros.entities.event.ClienteActualizadoEvent;
import com.andinaseguros.entities.event.PolizaEmitidaEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IntegrationEventMapperTest {
    private final IntegrationEventMapper mapper =
            new IntegrationEventMapper(
                    new RabbitMqProperties(
                            "andina.insurance.events",
                            "andina.events",
                            "policy.issued.v1",
                            "andina.policy.audit.queue"));
    private final ObjectMapper json =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void policyIssuedConservaSuExchangeRoutingKeyYCampos() throws Exception {
        UUID polizaId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        var outbound =
                mapper.map(
                        new PolizaEmitidaEvent(
                                UUID.randomUUID(),
                                Instant.parse("2026-09-25T10:00:00Z"),
                                polizaId,
                                UUID.randomUUID(),
                                clienteId,
                                "POL-2026-ABCD1234"),
                        "corr-1");

        assertThat(outbound.exchange()).isEqualTo("andina.insurance.events");
        assertThat(outbound.routingKey()).isEqualTo("policy.issued.v1");
        JsonNode node = json.readTree(json.writeValueAsString(outbound.message()));
        assertThat(node.get("eventType").asText()).isEqualTo("PolicyIssued");
        assertThat(node.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(node.get("occurredAt").asText()).isEqualTo("2026-09-25T10:00:00Z");
        assertThat(node.get("correlationId").asText()).isEqualTo("corr-1");
        assertThat(node.at("/data/customerId").asText()).isEqualTo(clienteId.toString());
        assertThat(node.at("/data/policyNumber").asText()).isEqualTo("POL-2026-ABCD1234");
    }

    @Test
    void customerUpdatedSaleAlExchangeNuevoConLaVersionDelAgregado() throws Exception {
        UUID clienteId = UUID.randomUUID();
        var outbound =
                mapper.map(
                        new ClienteActualizadoEvent(
                                UUID.randomUUID(),
                                Instant.now(),
                                clienteId,
                                "DNI",
                                "70000001",
                                "Ana",
                                "Torres",
                                "ana@andina.local",
                                "921175206",
                                true,
                                7),
                        null);

        assertThat(outbound.exchange()).isEqualTo("andina.events");
        assertThat(outbound.routingKey()).isEqualTo("customer.updated.v1");
        JsonNode node = json.readTree(json.writeValueAsString(outbound.message()));
        assertThat(node.get("eventType").asText()).isEqualTo("CustomerUpdated");
        assertThat(node.get("aggregateVersion").asLong()).isEqualTo(7);
        assertThat(node.get("producer").asText()).isEqualTo("andina-backend");
        assertThat(node.at("/data/fullName").asText()).isEqualTo("Ana Torres");
        assertThat(node.at("/data/phone").asText()).isEqualTo("921175206");
    }
}
