# Guía de operación (paso 7.8 de la ruta)

Qué hacer para levantar, vigilar y reparar el sistema de microservicios. Está escrita para quien opera el sistema sin haberlo construido. El detalle de cómo se llegó aquí está en [n_IMPLEMENTACION-ENDURECIMIENTO-FASE7.md](n_IMPLEMENTACION-ENDURECIMIENTO-FASE7.md).

Todos los comandos se ejecutan desde la raíz del repositorio, salvo que se indique `Arquitectura-Clean/`.

---

## 1. Levantar el sistema

| Qué | Comando (desde `Arquitectura-Clean/`) |
|---|---|
| Stack completo | `docker compose up -d --build` |
| Con observabilidad (Grafana, Prometheus, Jaeger, Loki) | `docker compose -f docker-compose.yml -f ../infra/observability/docker-compose.observability.yml up -d` |
| WhatsApp y JSON.pe simulados (pruebas) | `WHATSAPP_BASE_URL=http://whatsapp-mock:8080 JSONPE_BASE_URL=http://jsonpe-mock:8080 docker compose --profile whatsapp-mock --profile jsonpe-mock up -d` |
| Con HTTPS local (https://localhost:8443 y :8444) | `FRONTEND_API_URL=https://localhost:8444/api FRONTEND_API_ORIGIN=https://localhost:8444 CORS_ALLOWED_ORIGINS=https://localhost:8443 docker compose --profile tls up -d --build` |
| Estado | `docker compose ps` (todos los servicios deben estar `healthy`) |

Puertos: frontend 5173, gateway 8080; con observabilidad, Grafana 3000, Jaeger 16686, Prometheus 9090 y Alertmanager 9093. Todo lo demás es interno. Más detalle en [DOCKER-EJECUCION.md](../../DOCKER-EJECUCION.md).

**Más de una réplica.** Todos los servicios admiten varias réplicas: el estado efímero vive en Redis y MongoDB, y el relay del Outbox publica solo desde la réplica que tiene el turno (`outbox_lock`). En Kubernetes basta con subir `replicas`. En Compose los servicios con `container_name` fijo no escalan con `--scale`; una segunda réplica de prueba se levanta con `docker compose run -d --no-deps <servicio>`.

## 2. Leer el tablero "Andina Seguros — Resumen" (Grafana)

| Panel | Qué mirar | Normal |
|---|---|---|
| Peticiones por segundo | Tráfico por servicio | Sigue al uso real |
| Respuestas 5xx por segundo | Errores del servidor | 0 |
| Latencia p95 | El 95 % de las peticiones responde por debajo de este tiempo | < 1 s (lecturas) |
| Circuit breakers abiertos | 1 = el circuito hacia ese destino está abierto | Vacío |
| Mensajes en DLQ | Mensajes que un consumidor no pudo procesar, por cola | 0 |
| Outbox: antigüedad del evento pendiente más viejo | Segundos que lleva sin publicarse el evento más viejo | 0 (o pocos segundos) |
| Consumo de pólizas pausado | 1 = notification dejó de consumir porque WhatsApp no responde | 0 |
| WhatsApp enviados por resultado | `success`, `rejected`, `unavailable` | Casi todo `success` |
| Servicios arriba | Prometheus puede leer cada servicio | Todos en 1 |
| Servicios listos (readiness) | El servicio tiene su MongoDB, RabbitMQ y Redis | Todos en verde |

**Seguir una petición.** Cada respuesta del gateway trae `X-Correlation-Id`. En Grafana, Explore, Loki: `{service=~".+"} |= "<correlationId>"` muestra las líneas de todos los servicios. Cada línea trae un `traceId`; en Jaeger, ese id muestra la traza completa (una emisión es una sola traza: gateway, policy y, por RabbitMQ, notification, claims y quotation).

## 3. Qué hacer ante cada alerta

Las alertas están en `infra/observability/prometheus/alertas.yml` y se ven en Prometheus (Alerts) y en Grafana. **Alertmanager** las envía por correo a ramirezlisset361@gmail.com, con el cuerpo predeterminado de Alertmanager (sin plantilla propia).

### 3.1 Envío de alertas por correo (Alertmanager)

| Qué | Detalle |
|---|---|
| Configuración | `infra/observability/alertmanager/alertmanager.yml`: Gmail (`smtp.gmail.com:587`, STARTTLS), remitente y destinatario ramirezlisset361@gmail.com, `send_resolved: true` (también avisa cuando se resuelve) |
| Contraseña | Una **contraseña de aplicación** de Google (no la contraseña de la cuenta), en `Arquitectura-Clean/.env` como `ALERTMANAGER_SMTP_PASSWORD=<16 letras sin espacios>`. Se crea en https://myaccount.google.com/apppasswords (requiere la verificación en 2 pasos activa). Después: `docker compose -f docker-compose.yml -f ../infra/observability/docker-compose.observability.yml up -d alertmanager` |
| Agrupación | Un correo por grupo de alerta y severidad; espera 30 s para juntar las que se disparan a la vez; si el grupo cambia, otro correo a los 5 min; si sigue activa, recordatorio cada 4 h |
| Consola | http://localhost:9093 (alertas recibidas, silencios). Para silenciar una alerta durante un mantenimiento: "New Silence" con su `alertname` |
| Si no llegan correos | `docker logs andina-alertmanager \| grep -i notify`. "missing password": falta la variable. "535 Username and Password not accepted": la contraseña de aplicación es incorrecta o se revocó. Revisar también la carpeta de spam |
| Probar el envío | Apagar un servicio 2 minutos (`docker stop claims-service`): llegan `ServicioCaido` y `ServicioNoListo`; al volver a levantarlo, el aviso de resueltas |

| Alerta | Qué significa | Qué hacer |
|---|---|---|
| **ServicioCaido** (`up == 0`, 1 min) | El proceso no responde | `docker compose ps` y `docker compose logs --tail 200 <servicio>`. Si salió por memoria o error, `docker compose up -d <servicio>`. Mientras tanto el gateway responde 503 en sus rutas y el resto del sistema sigue (ver sección 5) |
| **ServicioNoListo** (readiness, 1 min) | El proceso vive pero no alcanza su MongoDB, RabbitMQ o Redis | `docker exec andina-api-gateway wget -qO- http://<servicio>:8080/actuator/health/readiness` dice qué componente falla. Levantar ese componente; el servicio se recupera solo |
| **ErroresServidorAltos** (> 5 % de 5xx, 5 min) | Muchas respuestas de error | En Loki, `{service="<servicio>"} \|= "ERROR"`. Un 503 del gateway suele ser un circuito abierto o un servicio caído; un 500, un error de programación: abrir incidente con el `correlationId` |
| **CircuitoAbierto** (2 min) | Un destino falla y se dejó de llamarlo | Ver cuál (`name`): `customerCB`, `claimsCB`, `quotationCB`, `policyCB`, `identityCB` (gateway), `jsonpe` (placas), `whatsapp`, `customer` (quotation → customer). Arreglar el destino; el circuito se cierra solo tras 30 s en semiabierto |
| **DlqConMensajes** (1 min) | Un consumidor no pudo procesar mensajes | Sección 6 |
| **OutboxAtrasado** (> 5 min) | Un servicio no puede publicar eventos (RabbitMQ caído o inaccesible) | Levantar RabbitMQ. Los eventos no se pierden: el relay los publica en orden al volver. Si RabbitMQ está bien, revisar `lastError` en la colección `outbox` del servicio |
| **NotificacionesPausadas** (2 min) | WhatsApp no responde; notification retiene los mensajes (no van a la DLQ) | Revisar el proveedor (JSON.pe WhatsApp) y `WHATSAPP_TOKEN`. Al recuperarse, el consumo se reanuda solo y salen los retenidos |
| **CopiaRevocacionesAtrasada** (> 60 s, 1 min) | El gateway no puede refrescar su copia de la lista de revocación: Redis de identity no responde. Sigue rechazando lo revocado antes de la caída; lo revocado durante la caída no se conoce | Levantar Redis (`docker compose up -d redis`). La copia se pone al día sola en 5 s |

## 4. Reconciliación de proyecciones (cada noche)

Cada servicio guarda copias de datos de otros (proyecciones). La reconciliación compara cada copia con su fuente:

```bash
sh infra/operacion/reconciliar.sh     # 0 = todo coincide; 1 = hay diferencias
```

Programarla cada noche (cron en Linux, Programador de tareas en Windows) y avisar si sale con 1. Para cada diferencia:

| Resultado | Causa probable | Cómo corregir |
|---|---|---|
| **falta** (está en la fuente, no en la copia) | Un evento no llegó (DLQ, cola atrasada) | Revisar la DLQ del consumidor (sección 6). Para clientes y vehículos, reenviar: `POST /api/clientes/eventos/reenvio` (ADMIN); para siniestros, `POST /api/siniestros/eventos/reenvio` |
| **sobra** (está en la copia, no en la fuente) | La entidad se borró o nunca existió en la fuente (por ejemplo, datos de prueba que se reemplazaron al repetir una migración) | Confirmar que no existe en la fuente y borrar la copia: `db.<proyección>.deleteMany({_id: {$in: [...]}})` en la base del consumidor |
| **distinto** (mismo id, otros datos) | La copia descartó un evento por versión (tenía una versión igual o mayor de otra época) | Borrar la copia de ese id y reenviar desde la fuente (endpoints de arriba): se vuelve a crear con los datos actuales |

Para entrar a una base: `docker exec -it <contenedor-mongodb> mongosh -u root -p <clave root> --authenticationDatabase admin <base>` (claves en `.env`).

## 5. Qué sigue funcionando si algo se cae (degradación planificada)

Verificado con `bash infra/operacion/caos.sh` (casos básicos, ~25 min) y `bash infra/operacion/caos.sh todos` (suma 2 réplicas, lote con 2 réplicas e identity caído 6 min). Resultados en la sección 3.2 de `n_…`.

| Si cae… | Deja de funcionar | Sigue funcionando |
|---|---|---|
| customer-service | Clientes, vehículos, consulta de placas (503) | Cotizar clientes ya conocidos (copias en quotation), emitir, siniestros, renovar |
| quotation-service | Cotizar, tarifas (503) | Emitir cotizaciones ya aceptadas, siniestros, renovar |
| policy-service | Pólizas y renovaciones (503); "Mi cuenta" responde parcial | Clientes, cotizar y aceptar (el `quote.accepted` espera en la cola) |
| claims-service | Siniestros (503); **generar pólizas renovadas** (503 `SINIESTROS_NO_DISPONIBLE`: se confirma con claims y se prefiere no renovar a renovar con datos viejos) | Emitir, evaluar y aprobar renovaciones (usan `claim_ref`); `policy.issued` espera en la cola |
| notification-service | El WhatsApp (se envía al volver) | Todo lo demás |
| identity-service | Iniciar sesión (503); un servicio que se reinicie mientras identity está caído no puede validar tokens hasta que identity vuelva | Los tokens ya emitidos siguen valiendo hasta que expiran (probado con 6 min de caída) |
| RabbitMQ | La propagación de eventos (se retrasa) | Todas las operaciones; los eventos esperan en el Outbox |
| MongoDB de un servicio | Ese servicio (no listo, 503) | Los demás |
| Redis | El límite de tasa del gateway (se deja pasar). MFA y login social guardan su estado en Redis: se espera que fallen (no se probó) | Todo lo demás, incluido el login con contraseña. La revocación se decide con la copia local del gateway: lo revocado antes de la caída sigue rechazado |
| JSON.pe (placas) | Datos de placas nuevas: responde `SIN_DATOS` y se ingresan a mano | Placas ya consultadas (caché de 24 h) |
| WhatsApp | El envío (se retiene, no va a la DLQ) | Todo lo demás |

## 6. DLQ: ver, corregir y reprocesar

Cada consumidor tiene su cola `<cola>` y su DLQ `<cola>.dlq`. Un mensaje llega a la DLQ tras 3 reintentos fallidos o si es inválido.

1. **Ver la causa:** en el log del consumidor, `Mensaje <id> enviado a la DLQ: <causa>` (notification) o el `WARN` del listener. El contenido se ve en la consola de RabbitMQ (con `docker-compose.debug.yml`, http://localhost:15672, cola, "Get messages").
2. **Corregir la causa** (por ejemplo, reenviar el cliente que faltaba, corregir un token).
3. **Reprocesar:** `sh infra/rabbitmq/reprocesar-dlq.sh <cola>.dlq` mueve los mensajes de vuelta a la cola principal. Los consumidores son idempotentes: un mensaje ya aplicado no se aplica dos veces.
4. Si un mensaje no tiene arreglo (dato de prueba), se descarta con `docker exec andina-clean-rabbitmq-1 rabbitmqctl purge_queue <cola>.dlq`.

## 7. Respaldos y restauración

```bash
sh infra/mongo/respaldar.sh                              # respaldos/<fecha-hora>/ con un MANIFIESTO.txt
sh infra/mongo/probar-restauracion.sh respaldos/<fecha-hora>   # restaura en un MongoDB temporal y compara
```

- Programar el respaldo cada noche y conservar al menos 7 días; copiar `respaldos/` fuera del equipo. La carpeta no se versiona: tiene datos personales.
- Probar la restauración de un respaldo al menos una vez al mes; el script no toca las bases reales.
- **Restaurar de verdad** una base (pérdida de datos): detener el servicio, `docker exec -i <contenedor-mongodb> mongorestore --drop --archive --gzip -u root -p <clave> --authenticationDatabase admin < respaldos/<fecha>/<base>.archive.gz`, levantar el servicio y ejecutar la reconciliación (sección 4): las copias en otros servicios pueden haber quedado más nuevas que la fuente restaurada.
- El respaldo del monolito retirado está en `respaldos/monolito-andina_seguros_clean-2026-09-27.archive.gz`.

## 8. Carga

`infra/carga/carga.js` (k6) mide lecturas, emisión y el límite del gateway. Correrlo después de cambios de rendimiento (desde `Arquitectura-Clean/`, con WhatsApp simulado):

```bash
docker run --rm -i --network andina_gateway_network -e BASE=http://gateway:8080 grafana/k6:0.54.0 run - < ../infra/carga/carga.js
```

## 9. Kubernetes

Manifiestos en `k8s/` (orden de aplicación en `k8s/README.md`); en CI se validan con kubeconform. Los mismos procedimientos aplican con `kubectl exec -n andina-seguros <pod> -- <comando>` en lugar de `docker exec`.
