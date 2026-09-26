# Ejecución Docker — API Gateway, backend Clean, identity-service y notification-service

Las arquitecturas Onion y Hexagonal se retiraron del compose (ver commit `chore(docker): elimina servicios Onion y Hexagonal del compose raíz`). Hoy el stack local es: **frontend, API Gateway, backend Clean, identity-service, notification-service, un MongoDB por servicio (backend, identity, notification), RabbitMQ y Redis**, definidos en [Arquitectura-Clean/docker-compose.yml](Arquitectura-Clean/docker-compose.yml).

Detalle de cada fase de la migración a microservicios:

- Fase 0, API Gateway: [doc/5. Microservicios/e_IMPLEMENTACION-API-GATEWAY-FASE0.md](doc/5.%20Microservicios/e_IMPLEMENTACION-API-GATEWAY-FASE0.md).
- Fase 1, notification-service: [doc/5. Microservicios/f_IMPLEMENTACION-NOTIFICATION-SERVICE-FASE1.md](doc/5.%20Microservicios/f_IMPLEMENTACION-NOTIFICATION-SERVICE-FASE1.md).
- Fase 2, identity-service: [doc/5. Microservicios/g_IMPLEMENTACION-IDENTITY-SERVICE-FASE2.md](doc/5.%20Microservicios/g_IMPLEMENTACION-IDENTITY-SERVICE-FASE2.md).

## Puertos publicados al host

Solo el frontend y el gateway exponen puertos (paso 0.6 de la ruta). Todo lo demás vive en redes internas de Docker.

| Servicio | URL | Notas |
|---|---|---|
| **Frontend** | http://localhost:5173 | Único punto de entrada pensado para el usuario final |
| **API Gateway** | http://localhost:8080/api | Único punto de entrada de la API. Swagger del backend en `http://localhost:8080/swagger-ui.html` (solo en local) |
| Backend, MongoDB, RabbitMQ, identity-service, notification-service, Redis | sin puerto en el host | Para depurar, ver "Abrir puertos internos" más abajo |
| Grafana / Jaeger / Prometheus | http://localhost:3000 · http://localhost:16686 · http://localhost:9090 | Solo con el stack de observabilidad (ver abajo) |

Cada servicio tiene su propia base con su propio usuario (`readWrite` solo sobre ella): `andina_seguros_clean` (usuario `andina`, replica set `rs0`), `identity_db` (`identity`) y `notification_db` (`notification`). Las claves por defecto sirven solo en local; se cambian en `Arquitectura-Clean/.env` (ver `.env.example`).

> Nota: el `docker-compose.yml` de la raíz del repositorio es un subconjunto reducido (solo `clean-mongodb` + `clean-backend`, sin gateway ni RabbitMQ) pensado para levantar el backend a solas; para el stack completo usar siempre el de `Arquitectura-Clean/`.

## Levantar el stack completo

```bash
cd Arquitectura-Clean
docker compose up -d --build
docker compose ps
```

Ver logs (incluye el gateway):

```bash
docker compose logs -f gateway backend notification-service
```

La primera vez con identity-service (fase 2), copiar los usuarios que ya existían en el backend (los que se crearon antes de la fase 2):

```bash
sh services/identity-service/migracion/migrar-usuarios.sh   # desde la raíz del repositorio
```

Después, poblar las proyecciones de notification-service (contactos) e identity-service (correos de clientes) con los clientes que ya existen (backfill, requiere un usuario ADMIN):

```bash
curl -X POST http://localhost:8080/api/clientes/eventos/reenvio -H "Authorization: Bearer <token>"
```

Probar sin enviar WhatsApp reales (WhatsApp simulado con WireMock):

```bash
WHATSAPP_BASE_URL=http://whatsapp-mock:8080 docker compose --profile whatsapp-mock up -d
```

### Abrir puertos internos (solo para depurar)

```bash
docker compose -f docker-compose.yml -f docker-compose.debug.yml up -d
# MongoDB del backend: mongodb://root:<BACKEND_MONGO_ROOT_PASSWORD>@localhost:27020/?directConnection=true
# RabbitMQ: http://localhost:15672 · Backend directo: http://localhost:8083
```

### Observabilidad (Grafana, Prometheus, Loki, Jaeger)

```bash
docker compose -f docker-compose.yml -f ../infra/observability/docker-compose.observability.yml up -d
```

- **Grafana** (http://localhost:3000, usuario `admin`, clave `GRAFANA_ADMIN_PASSWORD` o `grafana-local`): tablero "Andina Seguros — Resumen" y, en Explore, los logs de todos los servicios. Para seguir una petición: `{service=~".+"} |= "<X-Correlation-Id>"`; desde cada log, el `traceId` abre la traza en Jaeger.
- **Jaeger** (http://localhost:16686): una sola traza recorre gateway → backend → RabbitMQ → identity-service / notification-service.
- **Prometheus** (http://localhost:9090): métricas y alertas (circuito abierto, DLQ con mensajes, Outbox atrasado, 5xx, servicio caído, notificaciones pausadas).

Detener sin borrar datos:

```bash
docker compose down
```

## Aislamiento

- Backend ↔ MongoDB del backend: red `andina_clean_network`.
- notification-service ↔ su MongoDB: red `andina_notification_data_network`. notification-service **no** está en `andina_clean_network`: no puede llegar a la base del backend.
- Backend / notification-service ↔ RabbitMQ: red `rabbitmq_network`.
- Gateway ↔ Backend: `andina_clean_network`. Gateway ↔ Redis: `andina_gateway_network`.
- Volúmenes: `andina_clean_mongo_data`, `andina_notification_mongo_data`, `rabbitmq_data`.

No ejecutar a la vez el Compose de la raíz y el de `Arquitectura-Clean/`: ambos usan el mismo nombre de red y de contenedor para Mongo y backend.
