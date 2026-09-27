package com.andinaseguros.quotation.interfaceadapters.out.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.andinaseguros.quotation.entities.event.CotizacionAceptadaEvent;
import com.andinaseguros.quotation.entities.model.ResultadoTarificacion;
import com.andinaseguros.quotation.entities.valueobject.Dinero;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** El mensaje debe cumplir contracts/events/quote.accepted.v1.schema.json. */
class IntegrationEventMapperTest {
    private final IntegrationEventMapper mapper =
            new IntegrationEventMapper(new RabbitMqProperties("andina.events", "andina.insurance.events"));
    private final ObjectMapper json =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void quoteAcceptedLlevaLoQuePolicyServiceNecesitaParaEmitir() throws Exception {
        UUID cotizacionId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        Dinero prima = Dinero.soles(new BigDecimal("1412.50"));
        var desglose =
                new ResultadoTarificacion(
                        Dinero.soles(new BigDecimal("1250")), Dinero.soles(new BigDecimal("1250")),
                        Dinero.soles(new BigDecimal("125")), Dinero.soles(new BigDecimal("37.5")),
                        Dinero.soles(BigDecimal.ZERO), prima, List.of());
        var outbound =
                mapper.map(
                        new CotizacionAceptadaEvent(
                                UUID.randomUUID(),
                                Instant.parse("2026-09-27T10:00:00Z"),
                                cotizacionId,
                                "COT-ABC12345",
                                clienteId,
                                UUID.randomUUID(),
                                prima.valor(),
                                "PEN",
                                UUID.randomUUID(),
                                LocalDateTime.of(2026, 9, 27, 10, 0),
                                LocalDateTime.of(2026, 10, 12, 10, 0),
                                desglose),
                        "corr-1");

        assertThat(outbound.exchange()).isEqualTo("andina.events");
        assertThat(outbound.routingKey()).isEqualTo("quote.accepted.v1");
        JsonNode node = json.readTree(json.writeValueAsString(outbound.message()));
        assertThat(node.get("eventType").asText()).isEqualTo("QuoteAccepted");
        assertThat(node.get("aggregateId").asText()).isEqualTo(cotizacionId.toString());
        assertThat(node.get("aggregateVersion").asLong()).isEqualTo(1);
        assertThat(node.get("producer").asText()).isEqualTo("quotation-service");
        assertThat(node.at("/data/quoteNumber").asText()).isEqualTo("COT-ABC12345");
        assertThat(node.at("/data/customerId").asText()).isEqualTo(clienteId.toString());
        assertThat(node.at("/data/premium").decimalValue()).isEqualByComparingTo("1412.50");
        assertThat(node.at("/data/currency").asText()).isEqualTo("PEN");
        assertThat(node.at("/data/expiresAt").asText()).isEqualTo("2026-10-12T10:00:00");
        assertThat(node.at("/data/breakdown/primaComercial/valor").decimalValue()).isEqualByComparingTo("1412.50");
    }
}
