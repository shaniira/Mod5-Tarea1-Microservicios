package com.andinaseguros.policy.interfaceadapters.out.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.andinaseguros.policy.entities.enums.EstadoPoliza;
import com.andinaseguros.policy.entities.event.EmisionRechazadaEvent;
import com.andinaseguros.policy.entities.event.PolizaEmitidaEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** policy.* debe cumplir contracts/events: los consumidores actuales no cambian. */
class IntegrationEventMapperTest {
    private final IntegrationEventMapper mapper = new IntegrationEventMapper(new RabbitMqProperties("andina.events"));
    private final ObjectMapper json =
            new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void policyIssuedConservaLosCamposQueLeenNotificationClaimsYQuotation() throws Exception {
        UUID poliza = UUID.randomUUID();
        UUID cotizacion = UUID.randomUUID();
        UUID cliente = UUID.randomUUID();
        var outbound =
                mapper.map(
                        new PolizaEmitidaEvent(
                                UUID.randomUUID(), Instant.parse("2026-09-27T10:00:00Z"), poliza, cotizacion, cliente,
                                "POL-2026-ABCD1234", UUID.randomUUID(), new BigDecimal("1412.50"), "PEN",
                                LocalDate.of(2026, 10, 1), LocalDate.of(2027, 10, 1), EstadoPoliza.VIGENTE, 1),
                        "corr-1");

        assertThat(outbound.exchange()).isEqualTo("andina.events");
        assertThat(outbound.routingKey()).isEqualTo("policy.issued.v1");
        JsonNode node = json.readTree(json.writeValueAsString(outbound.message()));
        assertThat(node.get("eventType").asText()).isEqualTo("PolicyIssued");
        assertThat(node.get("aggregateId").asText()).isEqualTo(poliza.toString());
        assertThat(node.get("aggregateVersion").asLong()).isEqualTo(1);
        assertThat(node.at("/data/policyId").asText()).isEqualTo(poliza.toString());
        assertThat(node.at("/data/quoteId").asText()).isEqualTo(cotizacion.toString());
        assertThat(node.at("/data/customerId").asText()).isEqualTo(cliente.toString());
        assertThat(node.at("/data/policyNumber").asText()).isEqualTo("POL-2026-ABCD1234");
        assertThat(node.at("/data/startDate").asText()).isEqualTo("2026-10-01");
        assertThat(node.at("/data/status").asText()).isEqualTo("VIGENTE");
    }

    @Test
    void laCompensacionLlevaLaCotizacionYElMotivo() throws Exception {
        UUID cotizacion = UUID.randomUUID();
        var outbound =
                mapper.map(
                        new EmisionRechazadaEvent(
                                UUID.randomUUID(), Instant.now(), cotizacion, "COTIZACION_VENCIDA", "venció", null),
                        null);

        assertThat(outbound.routingKey()).isEqualTo("policy.issuance-rejected.v1");
        JsonNode node = json.readTree(json.writeValueAsString(outbound.message()));
        assertThat(node.get("eventType").asText()).isEqualTo("PolicyIssuanceRejected");
        assertThat(node.at("/data/quoteId").asText()).isEqualTo(cotizacion.toString());
        assertThat(node.at("/data/reasonCode").asText()).isEqualTo("COTIZACION_VENCIDA");
    }
}
