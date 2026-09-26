package com.andinaseguros.interfaceadapters.out.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.andinaseguros.interfaceadapters.out.persistence.mongodb.document.OutboxEventDocument;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.repository.SpringDataOutboxMongoRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.mockito.ArgumentCaptor;

class OutboxRelayTest {
    private final SpringDataOutboxMongoRepository outbox = mock(SpringDataOutboxMongoRepository.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-25T10:00:00Z"), ZoneOffset.UTC);
    private final OutboxRelay relay = new OutboxRelay(outbox, rabbit, 100, Duration.ofSeconds(1), clock);

    @Test
    void marcaComoEnviadoSoloCuandoRabbitConfirma() {
        OutboxEventDocument evento = pendiente("e1");
        when(outbox.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any())).thenReturn(List.of(evento));
        doAnswer(invocation -> {
                    CorrelationData correlation = invocation.getArgument(3);
                    correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
                    return null;
                })
                .when(rabbit)
                .send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        int enviados = relay.publicarPendientes();

        assertThat(enviados).isEqualTo(1);
        assertThat(evento.status).isEqualTo("SENT");
        assertThat(evento.sentAt).isEqualTo(clock.instant());
        ArgumentCaptor<Message> mensaje = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(eq("andina.events"), eq("customer.registered.v1"), mensaje.capture(), any(CorrelationData.class));
        assertThat(mensaje.getValue().getMessageProperties().getMessageId()).isEqualTo("e1");
        assertThat((String) mensaje.getValue().getMessageProperties().getHeader("X-Correlation-Id")).isEqualTo("corr-1");
    }

    @Test
    void siRabbitEstaCaidoElEventoQuedaPendienteYNoSeAdelantanLosSiguientes() {
        OutboxEventDocument primero = pendiente("e1");
        OutboxEventDocument segundo = pendiente("e2");
        when(outbox.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any()))
                .thenReturn(List.of(primero, segundo));
        doThrow(new AmqpConnectException(new java.net.ConnectException("Connection refused")))
                .when(rabbit)
                .send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        int enviados = relay.publicarPendientes();

        assertThat(enviados).isZero();
        assertThat(primero.status).isEqualTo("PENDING");
        assertThat(primero.attempts).isEqualTo(1);
        assertThat(primero.lastError).contains("AmqpConnectException");
        assertThat(segundo.attempts).isZero();
        verify(rabbit, times(1)).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    @Test
    void siRabbitNoConfirmaElEventoQuedaPendiente() {
        OutboxEventDocument evento = pendiente("e1");
        when(outbox.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any())).thenReturn(List.of(evento));
        doAnswer(invocation -> {
                    CorrelationData correlation = invocation.getArgument(3);
                    correlation.getFuture().complete(new CorrelationData.Confirm(false, "nack"));
                    return null;
                })
                .when(rabbit)
                .send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        relay.publicarPendientes();

        assertThat(evento.status).isEqualTo("PENDING");
        assertThat(evento.lastError).contains("nack");
    }

    private static OutboxEventDocument pendiente(String id) {
        OutboxEventDocument d = new OutboxEventDocument();
        d.id = id;
        d.exchange = "andina.events";
        d.routingKey = "customer.registered.v1";
        d.eventType = "CustomerRegistered";
        d.payload = "{\"eventId\":\"" + id + "\"}";
        d.correlationId = "corr-1";
        d.status = OutboxEventDocument.PENDING;
        d.createdAt = Instant.parse("2026-09-25T09:59:00Z");
        return d;
    }
}
