# Contratos

Esquemas de los eventos que viajan por RabbitMQ entre el backend y los microservicios (sección 4 de la [propuesta de migración](../doc/5.%20Microservicios/c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md)).

| Evento (routing key) | Exchange | Publica | Consume | Esquema |
|---|---|---|---|---|
| `customer.registered.v1` | `andina.events` | customer-service (hasta la fase 2: backend) | notification-service, identity-service, backend | [events/customer.registered.v1.schema.json](events/customer.registered.v1.schema.json) |
| `customer.updated.v1` | `andina.events` | customer-service (hasta la fase 2: backend) | notification-service, identity-service, backend | [events/customer.updated.v1.schema.json](events/customer.updated.v1.schema.json) |
| `vehicle.registered.v1` | `andina.events` | customer-service | backend (mientras cotiza), quotation-service (fase 5) | [events/vehicle.registered.v1.schema.json](events/vehicle.registered.v1.schema.json) |
| `policy.issued.v1` | `andina.insurance.events` (backend) y `andina.events` (policy-service) | backend (fase 6: policy-service) | notification-service, claims-service (proyección `policy_ref`), quotation-service (marca la cotización EMITIDA), auditoría | [events/policy.issued.v1.schema.json](events/policy.issued.v1.schema.json) |
| `policy.renewed.v1` | `andina.events` | policy-service | claims-service | [events/policy.renewed.v1.schema.json](events/policy.renewed.v1.schema.json) |
| `policy.issuance-rejected.v1` | `andina.events` | policy-service | quotation-service | [events/policy.issuance-rejected.v1.schema.json](events/policy.issuance-rejected.v1.schema.json) |
| `policy.expired.v1` / `policy.cancelled.v1` | `andina.events` | policy-service (sin productor todavía) | claims-service | [expired](events/policy.expired.v1.schema.json), [cancelled](events/policy.cancelled.v1.schema.json) |
| `quote.accepted.v1` | `andina.events` | quotation-service | policy-service (proyección `accepted_quotes`) | [events/quote.accepted.v1.schema.json](events/quote.accepted.v1.schema.json) |
| `claim.registered.v1` | `andina.events` | claims-service | policy-service (proyección `claim_ref`) | [events/claim.registered.v1.schema.json](events/claim.registered.v1.schema.json) |
| `claim.status-changed.v1` | `andina.events` | claims-service | policy-service (proyección `claim_ref`) | [events/claim.status-changed.v1.schema.json](events/claim.status-changed.v1.schema.json) |

## Reglas

- **Sobre común:** `eventId`, `eventType`, `eventVersion`, `occurredAt`, `aggregateId`, `data`, más los campos opcionales `aggregateVersion`, `correlationId` y `producer`.
- **Solo cambios compatibles:** se pueden agregar campos opcionales. Un cambio incompatible crea `…v2` y se publican las dos versiones durante la transición.
- **Idempotencia:** los consumidores guardan el `eventId` en su `inbox` y no repiten el efecto si el evento llega dos veces.
- **Orden:** los eventos de un agregado llevan `aggregateVersion`; el consumidor descarta los que tengan una versión igual o menor a la que ya aplicó.
- Los mensajes llevan además el encabezado AMQP `X-Correlation-Id` y el `messageId` igual al `eventId`.

## APIs REST (OpenAPI)

| Servicio | Contrato | Cómo se regenera |
|---|---|---|
| identity-service | [openapi/identity-service.json](openapi/identity-service.json) | `GET /v3/api-docs` del servicio (springdoc), por ejemplo `docker exec andina-api-gateway wget -qO- http://identity-service:8080/v3/api-docs` |
| customer-service | [openapi/customer-service.json](openapi/customer-service.json) | `docker exec andina-api-gateway wget -qO- http://customer-service:8080/v3/api-docs` |
| claims-service | [openapi/claims-service.json](openapi/claims-service.json) | `docker exec andina-api-gateway wget -qO- http://claims-service:8080/v3/api-docs` |
| quotation-service | [openapi/quotation-service.json](openapi/quotation-service.json) | `docker exec andina-api-gateway wget -qO- http://quotation-service:8080/v3/api-docs` |
| policy-service | [openapi/policy-service.json](openapi/policy-service.json) | `docker exec andina-api-gateway wget -qO- http://policy-service:8080/v3/api-docs` |
| backend (monolito) | `GET /v3/api-docs` en `http://localhost:8083` | Se exportará al separar cada dominio (fases 3 a 6) |
