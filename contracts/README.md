# Contratos

Esquemas de los eventos que viajan por RabbitMQ entre los microservicios (sección 4 de la [propuesta de migración](../doc/5.%20Microservicios/c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md)).

| Evento (routing key) | Exchange | Publica | Consume | Esquema |
|---|---|---|---|---|
| `customer.registered.v1` | `andina.events` | customer-service | notification-service, identity-service, quotation-service (`customer_ref`) | [events/customer.registered.v1.schema.json](events/customer.registered.v1.schema.json) |
| `customer.updated.v1` | `andina.events` | customer-service | notification-service, identity-service, quotation-service (`customer_ref`) | [events/customer.updated.v1.schema.json](events/customer.updated.v1.schema.json) |
| `vehicle.registered.v1` | `andina.events` | customer-service | quotation-service (`vehicle_ref`) | [events/vehicle.registered.v1.schema.json](events/vehicle.registered.v1.schema.json) |
| `policy.issued.v1` | `andina.events` (el heredado `andina.insurance.events` se retiró en el paso 6.11) | policy-service | notification-service, claims-service (proyección `policy_ref`), quotation-service (marca la cotización EMITIDA) | [events/policy.issued.v1.schema.json](events/policy.issued.v1.schema.json) |
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
- **Fechas:** `date-time` en RFC 3339 con zona (`2026-10-12T10:00:00Z`). Sin zona no cumple el contrato (la prueba de contrato lo detectó en `quote.accepted`, fase 7).

## Pruebas de contrato (fase 7, paso 7.6)

Corren en CI con cada cambio de un servicio o de `contracts/`; un cambio incompatible rompe el build (`CONTRATOS_OBLIGATORIOS=true` en los workflows). Al construir las imágenes Docker se saltan, porque el contexto no incluye `contracts/`.

| Lado | Qué comprueba | Dónde |
|---|---|---|
| Productor | Cada evento que genera el servicio cumple su esquema (formato de fechas y uuid incluidos) | `IntegrationEventMapperTest` de customer, claims, quotation y policy |
| Consumidor | El listener acepta el ejemplo de cada evento que consume y llega al caso de uso | `ContratoEventosConsumidosTest` de notification, identity, quotation, claims y policy |
| Ejemplos | Un evento válido por esquema, tal como lo publica su productor | [events/ejemplos/](events/ejemplos) (se validan contra el esquema en cada prueba) |
| API | Los controladores coinciden con el OpenAPI: falla si se quita o renombra un endpoint, si una respuesta pierde un campo, si el cuerpo pide un campo obligatorio nuevo o si hay un endpoint sin contrato | `ContratoApiTest` de customer, claims, quotation, policy e identity |

Al cambiar un evento: primero el esquema y su ejemplo aquí, luego productor y consumidores. Al agregar un endpoint: regenerar el OpenAPI del servicio (tabla siguiente).

## APIs REST (OpenAPI)

| Servicio | Contrato | Cómo se regenera |
|---|---|---|
| identity-service | [openapi/identity-service.json](openapi/identity-service.json) | `GET /v3/api-docs` del servicio (springdoc), por ejemplo `docker exec andina-api-gateway wget -qO- http://identity-service:8080/v3/api-docs` |
| customer-service | [openapi/customer-service.json](openapi/customer-service.json) | `docker exec andina-api-gateway wget -qO- http://customer-service:8080/v3/api-docs` |
| claims-service | [openapi/claims-service.json](openapi/claims-service.json) | `docker exec andina-api-gateway wget -qO- http://claims-service:8080/v3/api-docs` |
| quotation-service | [openapi/quotation-service.json](openapi/quotation-service.json) | `docker exec andina-api-gateway wget -qO- http://quotation-service:8080/v3/api-docs` |
| policy-service | [openapi/policy-service.json](openapi/policy-service.json) | `docker exec andina-api-gateway wget -qO- http://policy-service:8080/v3/api-docs` |
| backend (monolito) | Retirado en el paso 6.10 | Sus dominios quedaron en los contratos de customer, claims, quotation y policy |
