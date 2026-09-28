# notification-service

Primer microservicio de la migración (fase 1). Envía por WhatsApp (JSON.pe) el aviso de póliza emitida. Antes se llamaba `notification-consumer` y leía el teléfono directamente de la colección `clientes` del backend; ahora tiene **su propia base** y una **copia local de los contactos** que mantiene con eventos.

Diseño completo, decisiones y pruebas realizadas: [doc/5. Microservicios/f_IMPLEMENTACION-NOTIFICATION-SERVICE-FASE1.md](../../doc/5.%20Microservicios/f_IMPLEMENTACION-NOTIFICATION-SERVICE-FASE1.md).

## Flujo

```text
customer-service / policy-service ──(Outbox)──► RabbitMQ (andina.events)
   customer.registered.v1 / customer.updated.v1  (customer-service)
        └─► notification.customer.events ─► CustomerEventsListener ─► customer_contacts (notification_db)
   policy.issued.v1  (policy-service; el exchange heredado andina.insurance.events se retiró en el paso 6.11)
        └─► notification.policy.events ─► PolicyIssuedListener
                ─► teléfono desde customer_contacts ─► WhatsApp (timeout + retry + circuit breaker)
```

## Estructura (Clean Architecture, verificada con ArchUnit)

| Paquete | Contenido |
|---|---|
| `entities` | `ContactoCliente`, `Notificacion` |
| `usecases` | `NotificarPolizaEmitidaUseCase`, `ActualizarContactoClienteUseCase`, puertos de salida y excepciones |
| `interfaceadapters.in.messaging` | Listeners de RabbitMQ y contratos de los mensajes |
| `interfaceadapters.out` | MongoDB (`customer_contacts`, `inbox`) y WhatsApp (JSON.pe) |
| `frameworksdrivers.config` | Topología de RabbitMQ, manejo de fallos, pausa del listener, monitor de DLQ |

## Qué pasa con un mensaje que falla

| Caso | Qué hace |
|---|---|
| WhatsApp caído (timeout, conexión, 5xx, 429) | Hasta 3 intentos con espera exponencial. Si sigue fallando, el mensaje **vuelve a la cola**. Si el circuit breaker se abre, el listener de pólizas **se pausa** y los mensajes esperan en la cola; se reanuda solo cuando el circuito pasa a semiabierto (30 s por defecto) |
| El contacto aún no está en la proyección | 3 intentos (1 s, 2 s) por si el evento `customer.*` viene en camino; después, a la DLQ |
| JSON.pe rechaza el mensaje (4xx, `success=false`) o el evento es inválido | Directo a la DLQ (reintentar no cambia nada) |
| Evento repetido | Se confirma sin repetir el envío (`inbox`) |
| Evento de cliente con versión vieja | Se ignora (`aggregateVersion`) |

## Configuración

| Variable | Por defecto | Uso |
|---|---|---|
| `SPRING_DATA_MONGODB_URI` | `mongodb://localhost:27017/notification_db` | Base propia (en Compose, con el usuario `notification`) |
| `SPRING_RABBITMQ_HOST` / `_PORT` / `_USERNAME` / `_PASSWORD` | `localhost` / `5672` / `andina` / `andina-local` | RabbitMQ |
| `WHATSAPP_BASE_URL` | `https://api.whatsapp.json.pe` | API de WhatsApp (o `http://whatsapp-mock:8080` para pruebas) |
| `WHATSAPP_TOKEN` | vacío | Token de JSON.pe. Nunca en el repositorio |
| `WHATSAPP_TIMEOUT_SECONDS` | `5` | Timeout de conexión y lectura |
| `WHATSAPP_CB_WAIT_OPEN` | `30s` | Tiempo con el circuito abierto antes de probar de nuevo |
| `MESSAGE_RETRY_MAX_ATTEMPTS` | `3` | Intentos por mensaje para fallos no relacionados con WhatsApp |
| `MANAGEMENT_OTLP_TRACING_ENDPOINT` | sin definir | Si se define (p. ej. `http://otel-collector:4318/v1/traces`), exporta las trazas por OTLP. Sin él, el `traceId` igual aparece en el log |
| `SPRING_PROFILES_ACTIVE=local` | — | Log en texto en vez de JSON |

## Ejecución

Con Docker (desde la raíz del repositorio):

```bash
docker compose up -d --build notification-service
docker compose logs -f notification-service
```

Con WhatsApp simulado (WireMock, no envía mensajes reales):

```bash
WHATSAPP_BASE_URL=http://whatsapp-mock:8080 docker compose --profile whatsapp-mock up -d
```

Local (Java 21 y Maven), contra el RabbitMQ del Compose y un Mongo propio:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Pruebas (unitarias y reglas ArchUnit; también corren al construir la imagen):

```bash
mvn test
```

### Primera puesta en marcha: backfill de contactos

La proyección `customer_contacts` nace vacía. Para cargar los clientes que ya existían, un ADMIN llama una vez al backend (se puede repetir sin riesgo):

```bash
curl -X POST http://localhost:8080/api/clientes/eventos/reenvio -H "Authorization: Bearer <token>"
# 202 {"clientesPublicados": N}
```

## Salud y métricas

Sin puerto en el host; se consultan desde el contenedor:

```bash
docker exec notification-service wget -qO- http://localhost:8080/actuator/health/readiness
docker exec notification-service wget -qO- http://localhost:8080/actuator/prometheus | grep -E "^notification_|circuitbreaker_state"
```

| Métrica | Significado | Alerta sugerida |
|---|---|---|
| `notification_dlq_messages{queue}` | Mensajes en cada DLQ | > 0 |
| `notification_listener_paused` | 1 si el consumo de pólizas está pausado | = 1 durante más de 2 min |
| `resilience4j_circuitbreaker_state{name="whatsapp"}` | Estado del circuito | `open` durante más de 2 min |
| `notification_whatsapp_sent_total{result}` | Envíos `success`, `rejected`, `unavailable` | Tasa de `rejected` en aumento |

*Readiness* depende de MongoDB y RabbitMQ; *liveness* solo del proceso.

## Reprocesar la DLQ

Cuando `notification_dlq_messages` es mayor que 0 (también aparece un `WARN` "La DLQ ... tiene N mensaje(s)" en el log):

1. **Ver la causa.** En el log está el motivo de cada mensaje (`Mensaje <id> enviado a la DLQ: <causa>`). En la consola de RabbitMQ (http://localhost:15672, cola `notification.policy.events.dlq` o `notification.customer.events.dlq`, "Get messages") se ve el contenido.
2. **Corregir la causa.** Ejemplos: si falta el contacto, ejecutar el backfill (arriba); si el token de WhatsApp era incorrecto, corregir `WHATSAPP_TOKEN` y reiniciar el servicio.
3. **Mover los mensajes de vuelta a la cola principal** con el script común (un shovel que se borra solo al vaciar la DLQ; el plugin ya viene activo desde la fase 7):

   ```bash
   sh infra/rabbitmq/reprocesar-dlq.sh notification.policy.events.dlq
   sh infra/rabbitmq/reprocesar-dlq.sh notification.customer.events.dlq
   ```

   También se puede usar "Move messages" en la consola de RabbitMQ.
4. **Comprobar** que la DLQ quedó en 0. Los mensajes que ya se habían enviado no se repiten (`inbox`).

Si un mensaje no tiene arreglo (por ejemplo, una póliza de prueba), se descarta con `rabbitmqctl purge_queue <dlq>`.
