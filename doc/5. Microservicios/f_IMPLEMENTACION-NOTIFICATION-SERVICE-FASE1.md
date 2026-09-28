# Andina Seguros — Implementación de notification-service (Fase 1 de la migración a microservicios)

Este documento registra lo que se implementó en la fase 1 de la [ruta de implementación](d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md#5-fase-1--notification-service), las decisiones tomadas y la verificación de los criterios de aceptación contra el stack de Docker.

**Resultado:** el consumidor de notificaciones es ahora el microservicio `services/notification-service`. Tiene base propia (`notification_db`), no tiene acceso a la base del backend, obtiene el teléfono de una proyección que mantiene con eventos `customer.*`, y tolera la caída de WhatsApp sin perder mensajes ni llenar la DLQ.

---

## 1. Qué se hizo, paso por paso

| Paso de la ruta | Implementación |
|---|---|
| 1.1 Eventos de cliente desde el monolito | El backend publica `customer.registered.v1` al crear un cliente y `customer.updated.v1` al cambiar su contacto (endpoint nuevo `PATCH /api/clientes/{id}/contacto`, roles ADMIN/AGENTE). Salen por el exchange nuevo `andina.events` y llevan la versión del cliente (`aggregateVersion`) |
| 1.1 "con Outbox" | Se construyó el Outbox (adelantando los pasos 0.7 y 0.8 de la fase 0): colección `outbox`, relay con *publisher confirms*, y transacción de MongoDB que guarda el cambio y el evento juntos. También se aplicó a `EmitirPolizaUseCase`, así que `policy.issued.v1` tampoco se pierde si RabbitMQ está caído (riesgo A1) |
| 1.2 Backfill | `POST /api/clientes/eventos/reenvio` (solo ADMIN) publica un `customer.registered.v1` por cliente, con su versión actual. Se puede repetir sin riesgo |
| 1.3 Base propia | Contenedor `notification-mongodb` con la base `notification_db` y las colecciones `customer_contacts` (proyección) e `inbox` (reemplaza a `processed_notification_events`) |
| 1.4 Consumidor de `customer.*` | `CustomerEventsListener` (un solo consumidor, para respetar el orden) actualiza `customer_contacts` con un *upsert* condicional: solo escribe si la versión que llega es mayor que la guardada |
| 1.5 Teléfono desde la proyección | `NotificarPolizaEmitidaUseCase` lee `customer_contacts`. Se eliminó `ClienteDocument` (la entidad del backend); una regla ArchUnit impide que vuelva |
| 1.6 Circuit breaker y pausa del listener | Resilience4j alrededor de la llamada a WhatsApp (timeout 5 s, 3 intentos con espera exponencial, circuit breaker). Si el circuito se abre, el listener de pólizas **se detiene** y los mensajes esperan en la cola; al pasar a semiabierto se reanuda |
| 1.7 DLQ operativa | Métrica `notification_dlq_messages{queue}` y `WARN` en el log cuando la DLQ tiene mensajes. Procedimiento de reproceso con *shovel* documentado (y probado) en el [README del servicio](../../services/notification-service/README.md#reprocesar-la-dlq) |
| 1.8 Plantilla del servicio | Estructura Clean con reglas ArchUnit, *readiness* (Mongo + RabbitMQ) y *liveness*, logs JSON con `correlationId`/`traceId`/`spanId`, métricas en `/actuator/prometheus`, trazas con Micrometer + OpenTelemetry, imagen multi-etapa sin root y sin puertos en el host, entrada en Compose con *healthcheck*. Las colas escuchan **los dos exchanges** (`andina.insurance.events` y `andina.events`) |
| 1.9 Sin acceso a la base del backend | El servicio usa el usuario `notification` (rol `readWrite` solo sobre `notification_db`) y ya no está en la red `andina_clean_network` |

Además:

- **Contratos:** esquemas JSON de los tres eventos en [`contracts/events`](../../contracts/README.md).
- **Correlación:** el backend toma el `X-Correlation-Id` del gateway (filtro nuevo), el Outbox lo copia al evento y notification-service lo pone en su log. Un mismo id recorre gateway → backend → RabbitMQ → notification-service.
- **Propiedad de las colas:** el backend ya no declara las colas de notificación; cada consumidor declara las suyas.

## 2. Decisiones

| Decisión | Motivo |
|---|---|
| **Construir el Outbox ahora** (aunque es de la fase 0) | La fase 1 lo exige para los eventos de cliente. Sin él, un cliente creado con RabbitMQ caído nunca llegaría a la proyección |
| **Replica set de un nodo en Compose** para el MongoDB del backend | Las transacciones multi-documento de MongoDB solo existen en un replica set. Los datos del volumen existente se conservan |
| **Transacción "si se puede"** (`MongoTransaccionAdapter`) | En Kubernetes el MongoDB sigue siendo un servidor suelto. Ahí el backend ejecuta la operación sin transacción y lo avisa una vez en el log, en vez de fallar. El Outbox igual evita perder eventos cuando RabbitMQ está caído |
| **Instancia de MongoDB separada** para notification (no una base más en la del backend) | La del backend no tiene autenticación todavía (paso 0.6 pendiente); con una instancia propia sí se puede exigir usuario y contraseña, y el aislamiento es también de red |
| **Pausar el listener** en vez de una cola de reintento con TTL | Es la primera opción de la propuesta (sección 6.3), no cambia los argumentos de la cola existente (cambiarlos obligaría a borrarla) y no consume reintentos mientras WhatsApp está caído |
| **Se conserva el nombre `andina.policy.notification.queue`** | Renombrarla habría perdido los mensajes que estuvieran en ella. El nombre sigue la convención antigua; se puede migrar cuando `policy.issued` pase a `andina.events` |
| **Eventos "gordos"** con el estado completo del cliente | El consumidor no tiene que volver a llamar al backend |
| **Backfill por endpoint** | Permite poblar o reconstruir la proyección en cualquier momento, también después de reprocesar la DLQ |

## 3. Verificación de los criterios de aceptación

Pruebas hechas el 2026-09-25 contra el stack de `Arquitectura-Clean/docker-compose.yml`, entrando por el gateway (`:8080`) y con WhatsApp simulado (`--profile whatsapp-mock`).

| # | Criterio de salida (ruta, sección 5) | Cómo se probó | Resultado |
|---|---|---|---|
| 1 | Con MongoDB del backend inaccesible para notification-service, las notificaciones siguen funcionando | Desde el contenedor, `nslookup mongodb` → `NXDOMAIN` (no hay ruta de red). El usuario `notification` recibe `Unauthorized` fuera de `notification_db`. Con eso, el flujo completo cliente → cambio de teléfono → póliza → WhatsApp funcionó | ✅ Cumple |
| 2 | Un cliente nuevo aparece en `customer_contacts` en segundos; un cliente existente aparece tras el backfill | Cliente creado por la API: en la proyección en ~1 s (versión 1); tras `PATCH /contacto`, versión 2 con el teléfono nuevo. Backfill: `202 {"clientesPublicados":4}` y los 4 clientes existentes en la proyección | ✅ Cumple |
| 3 | Con WhatsApp caído, los mensajes se retienen (no van a la DLQ) y se envían al recuperarse | Mock respondiendo 503 y 4 pólizas emitidas: circuito `open`, `notification_listener_paused=1`, cola con **4 mensajes y 0 consumidores**, DLQ en **0**. Al restaurar el mock, el circuito pasó a semiabierto, el listener se reanudó y se enviaron las 4 notificaciones; circuito `closed`, DLQ en 0 | ✅ Cumple |
| 4 | Un evento repetido no envía el WhatsApp dos veces | Se republicó en RabbitMQ el `policy.issued.v1` exacto (mismo `eventId`) de una póliza ya notificada: log `DUPLICADO`, el mock siguió con el mismo número de envíos (11 → 11) | ✅ Cumple |
| — | Reversa (volver al consumer anterior) | La versión anterior está en el historial de git (`notification-consumer`); no se borró ningún dato de la base del backend | ✅ Disponible |

Pruebas adicionales:

| Prueba | Resultado |
|---|---|
| Evento de cliente desordenado (versión 1 llegando después de la 2, con otro `eventId`) | Descartado (`VERSION_ANTIGUA`); la proyección conservó la versión 2 |
| Outbox con RabbitMQ detenido | `POST /api/clientes` respondió 201; el evento quedó `PENDING` (`outbox_events_pending=1`, antigüedad visible en métrica). Al arrancar RabbitMQ se publicó solo y llegó a la proyección |
| DLQ | Una póliza de un cliente inexistente fue a la DLQ tras 3 intentos; `notification_dlq_messages=1` y `WARN` en el log. El *shovel* documentado la devolvió a la cola principal |
| La cola de pólizas escucha los dos exchanges | Un `policy.issued.v1` publicado en `andina.events` llegó a `andina.policy.notification.queue` |
| Correlación | Un `X-Correlation-Id` enviado al gateway apareció en el log JSON de notification-service junto con `traceId` y `spanId` |
| Pruebas automáticas | Backend: 77 pruebas (incluye ArchUnit). notification-service: 27 pruebas (incluye 8 reglas ArchUnit). Ambas corren al construir las imágenes |

## 4. Limitaciones conocidas

| Punto | Detalle |
|---|---|
| **Ventana de consistencia eventual** | Si el teléfono de un cliente cambia y en el mismo segundo se emite una póliza suya, el WhatsApp puede salir al teléfono anterior: los dos eventos viajan por colas distintas y la póliza puede procesarse antes que el cambio. Se observó una vez en las pruebas. La proyección queda correcta; solo afecta a ese mensaje. Es el costo aceptado de usar proyecciones (sección 5.1 de la propuesta) |
| MongoDB de Kubernetes | Sigue siendo un servidor suelto: allí el Outbox funciona sin transacción (aviso en el log). notification-service aún no tiene manifiestos de Kubernetes, igual que el consumer anterior |
| Trazas | El `traceId` se genera y se propaga, pero solo se exportan si se define `MANAGEMENT_OTLP_TRACING_ENDPOINT`; el stack de observabilidad (paso 0.9) no existe todavía. El backend aún no propaga `traceparent` en los mensajes |
| Alertas | Las métricas existen, pero no hay Prometheus ni Alertmanager que las evalúen (paso 0.9). **Resuelto:** Prometheus evalúa las alertas y Alertmanager las envía por correo (`n_…`, sección 11) |
| Relay del Outbox | Pensado para una réplica del backend, que hoy no puede escalar (riesgo A3) |
| MongoDB del backend | Sigue sin autenticación y con el puerto publicado (paso 0.6 pendiente) |

## 5. Archivos

| Área | Archivos |
|---|---|
| Backend: eventos | `entities/event/ClienteRegistradoEvent`, `ClienteActualizadoEvent`; `Cliente` con `version` |
| Backend: casos de uso | `CrearClienteUseCase` (publica), `ActualizarContactoClienteUseCase`, `PublicarClientesExistentesUseCase`, `EmitirPolizaUseCase` (transacción); puerto `TransaccionPort` |
| Backend: Outbox | `interfaceadapters/out/event/*` (`OutboxDomainEventPublisherAdapter`, `OutboxRelay`, `IntegrationEventMapper`), `OutboxEventDocument`, `MongoTransaccionAdapter`, `frameworksdrivers/.../OutboxConfig` |
| Backend: API | `ClienteController` (`PATCH /{id}/contacto`, `POST /eventos/reenvio`), `CorrelationIdFilter` |
| notification-service | `services/notification-service/**` |
| Infraestructura | `Arquitectura-Clean/docker-compose.yml` (replica set, `notification-mongodb`, `notification-service`, `whatsapp-mock`) |
| Contratos | `contracts/events/*.schema.json` |

## 6. Impacto en el monolito

Detalle por commit en [k_IMPACTO-EN-EL-MONOLITO.md](k_IMPACTO-EN-EL-MONOLITO.md#3-fase-1--notification-service).

| Qué | Commit | Cambio en `Arquitectura-Clean` |
|---|---|---|
| Se agregó | `f7ad09c` | Outbox transaccional (colección `outbox`, relay con publisher confirms, `TransaccionPort`); eventos `customer.registered/updated.v1` con la versión del cliente; `PATCH /api/clientes/{id}/contacto`; backfill `POST /api/clientes/eventos/reenvio`; `CorrelationIdFilter`. `EmitirPolizaUseCase` pasa a guardar póliza y evento en una transacción |
| Se desacopló | `f7ad09c` | El backend dejó de declarar las colas de notificación y su DLQ (son de notification-service). notification-service dejó de leer la colección `clientes` |
| Infraestructura | `f7d3b94` | MongoDB del backend como replica set de un nodo; el consumer salió de la red del backend |

## 7. Próximo paso sugerido

Fase 2 (`identity-service`). Antes conviene cerrar lo que quedó de la fase 0 y es barato: control de acceso completo (S1, S2), datos demo solo en `dev`, y autenticación en el MongoDB del backend.
