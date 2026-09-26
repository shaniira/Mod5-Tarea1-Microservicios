# Contratos

Esquemas de los eventos que viajan por RabbitMQ entre el backend y los microservicios (sección 4 de la [propuesta de migración](../doc/5.%20Microservicios/c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md)).

| Evento (routing key) | Exchange | Publica | Consume | Esquema |
|---|---|---|---|---|
| `customer.registered.v1` | `andina.events` | backend | notification-service, identity-service | [events/customer.registered.v1.schema.json](events/customer.registered.v1.schema.json) |
| `customer.updated.v1` | `andina.events` | backend | notification-service, identity-service | [events/customer.updated.v1.schema.json](events/customer.updated.v1.schema.json) |
| `policy.issued.v1` | `andina.insurance.events` (heredado) | backend | notification-service, auditoría | [events/policy.issued.v1.schema.json](events/policy.issued.v1.schema.json) |

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
| backend (monolito) | `GET /v3/api-docs` en `http://localhost:8083` | Se exportará al separar cada dominio (fases 3 a 6) |
