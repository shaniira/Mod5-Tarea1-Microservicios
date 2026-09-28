package com.backendseguros.policy.interfaceadapters.out.event;

import com.backendseguros.policy.entities.event.DomainEvent;
import com.backendseguros.policy.entities.event.EmisionRechazadaEvent;
import com.backendseguros.policy.entities.event.PolizaEmitidaEvent;
import com.backendseguros.policy.entities.event.PolizaRenovadaEvent;
import com.backendseguros.policy.interfaceadapters.out.event.IntegrationEventMessage.PolicyIssuanceRejectedData;
import com.backendseguros.policy.interfaceadapters.out.event.IntegrationEventMessage.PolicyIssuedData;
import com.backendseguros.policy.interfaceadapters.out.event.IntegrationEventMessage.PolicyRenewedData;

/**
 * Traduce un evento de dominio al mensaje del catálogo. Todo sale por andina.events; los
 * consumidores de policy.issued.v1 escuchan también el exchange heredado donde publica el backend.
 */
public class IntegrationEventMapper {
    public static final String PRODUCER = "policy-service";

    public record OutboundEvent(
            String exchange, String routingKey, IntegrationEventMessage message) {}

    private final RabbitMqProperties properties;

    public IntegrationEventMapper(RabbitMqProperties properties) {
        this.properties = properties;
    }

    public OutboundEvent map(DomainEvent event, String correlationId) {
        return switch (event) {
            case PolizaEmitidaEvent e ->
                    salida(
                            "policy.issued.v1",
                            new IntegrationEventMessage(
                                    e.eventId(), "PolicyIssued", 1, e.occurredAt(), e.polizaId(), e.version(),
                                    correlationId, PRODUCER,
                                    new PolicyIssuedData(
                                            e.polizaId(), e.cotizacionId(), e.clienteId(), e.numeroPoliza(),
                                            e.vehiculoId(), e.prima(), e.moneda(), e.inicio(), e.fin(),
                                            e.estado().name())));
            case PolizaRenovadaEvent e ->
                    salida(
                            "policy.renewed.v1",
                            new IntegrationEventMessage(
                                    e.eventId(), "PolicyRenewed", 1, e.occurredAt(), e.polizaAnteriorId(),
                                    e.versionPolizaAnterior(), correlationId, PRODUCER,
                                    new PolicyRenewedData(
                                            e.polizaNuevaId(), e.polizaAnteriorId(), e.renovacionId(), e.clienteId(),
                                            e.vehiculoId(), e.numeroPolizaNueva(), e.prima(), e.moneda(), e.inicio(),
                                            e.fin())));
            case EmisionRechazadaEvent e ->
                    salida(
                            "policy.issuance-rejected.v1",
                            new IntegrationEventMessage(
                                    e.eventId(), "PolicyIssuanceRejected", 1, e.occurredAt(), e.cotizacionId(), null,
                                    correlationId, PRODUCER,
                                    new PolicyIssuanceRejectedData(
                                            e.cotizacionId(), e.codigoMotivo(), e.motivo(), e.polizaExistenteId())));
            default -> throw new IllegalArgumentException("Evento de dominio no soportado: " + event.getClass());
        };
    }

    private OutboundEvent salida(String routingKey, IntegrationEventMessage mensaje) {
        return new OutboundEvent(properties.eventsExchange(), routingKey, mensaje);
    }
}
