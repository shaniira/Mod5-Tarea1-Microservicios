package com.andinaseguros.claims.interfaceadapters.out.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.andinaseguros.claims.entities.enums.EstadoSiniestro;
import com.andinaseguros.claims.entities.event.SiniestroEstadoCambiadoEvent;
import com.andinaseguros.claims.entities.event.SiniestroRegistradoEvent;
import com.andinaseguros.claims.contratos.Contratos;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** El mensaje debe cumplir contracts/events/claim.*.schema.json. */
class IntegrationEventMapperTest {
    private final IntegrationEventMapper mapper =
            new IntegrationEventMapper(new RabbitMqProperties("andina.events"));
    private final ObjectMapper json =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void claimRegisteredSaleAAndinaEventsConLoQueNecesitaLaRenovacion() throws Exception {
        UUID siniestroId = UUID.randomUUID();
        UUID polizaId = UUID.randomUUID();
        var outbound =
                mapper.map(
                        new SiniestroRegistradoEvent(
                                UUID.randomUUID(),
                                Instant.parse("2026-09-27T10:00:00Z"),
                                siniestroId,
                                polizaId,
                                null,
                                EstadoSiniestro.REPORTADO,
                                true,
                                LocalDate.of(2026, 9, 1),
                                "CHOQUE",
                                new BigDecimal("1500.00"),
                                "PEN",
                                true,
                                "LEVE",
                                1),
                        "corr-1");

        assertThat(outbound.exchange()).isEqualTo("andina.events");
        assertThat(outbound.routingKey()).isEqualTo("claim.registered.v1");
        JsonNode node = json.readTree(json.writeValueAsString(outbound.message()));
        Contratos.validarEvento(outbound.routingKey(), node);
        assertThat(node.get("eventType").asText()).isEqualTo("ClaimRegistered");
        assertThat(node.get("aggregateId").asText()).isEqualTo(siniestroId.toString());
        assertThat(node.get("aggregateVersion").asLong()).isEqualTo(1);
        assertThat(node.get("producer").asText()).isEqualTo("claims-service");
        assertThat(node.at("/data/policyId").asText()).isEqualTo(polizaId.toString());
        assertThat(node.at("/data/status").asText()).isEqualTo("REPORTADO");
        assertThat(node.at("/data/open").asBoolean()).isTrue();
        assertThat(node.at("/data/insuredResponsible").asBoolean()).isTrue();
        assertThat(node.at("/data/date").asText()).isEqualTo("2026-09-01");
    }

    @Test
    void claimStatusChangedLlevaElEstadoAnteriorYElNuevo() throws Exception {
        var outbound =
                mapper.map(
                        new SiniestroEstadoCambiadoEvent(
                                UUID.randomUUID(),
                                Instant.now(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                EstadoSiniestro.EN_EVALUACION,
                                EstadoSiniestro.RECHAZADO,
                                false,
                                false,
                                3),
                        null);

        assertThat(outbound.routingKey()).isEqualTo("claim.status-changed.v1");
        JsonNode node = json.readTree(json.writeValueAsString(outbound.message()));
        Contratos.validarEvento(outbound.routingKey(), node);
        assertThat(node.get("eventType").asText()).isEqualTo("ClaimStatusChanged");
        assertThat(node.get("aggregateVersion").asLong()).isEqualTo(3);
        assertThat(node.at("/data/previousStatus").asText()).isEqualTo("EN_EVALUACION");
        assertThat(node.at("/data/newStatus").asText()).isEqualTo("RECHAZADO");
        assertThat(node.at("/data/open").asBoolean()).isFalse();
    }
}
