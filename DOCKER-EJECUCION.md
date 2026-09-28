# Ejecución Docker — API Gateway y microservicios

Desde el cierre de la fase 6 (2026-09-27) el stack local es: **frontend, API Gateway, identity-service, notification-service, customer-service, claims-service, quotation-service y policy-service, un MongoDB por servicio, RabbitMQ y Redis**, definidos en [Arquitectura-Clean/docker-compose.yml](Arquitectura-Clean/docker-compose.yml). El monolito (`backend`) y su MongoDB ya no arrancan: quedan en el perfil `monolito` solo para una reversa (ver más abajo).

Detalle de cada fase de la migración: [doc/5. Microservicios/](doc/5.%20Microservicios/) (`e_` fase 0, `f_` fase 1, `g_` fase 2, `i_` fase 3, `j_` fase 4, `l_` fase 5, `m_` fase 6 y retiro del monolito).

## Puertos publicados al host

Solo el frontend y el gateway exponen puertos (paso 0.6 de la ruta). Todo lo demás vive en redes internas de Docker.

| Servicio | URL | Notas |
|---|---|---|
| **Frontend** | http://localhost:5173 | Único punto de entrada pensado para el usuario final |
| **API Gateway** | http://localhost:8080/api | Único punto de entrada de la API. Los contratos de cada servicio están en [contracts/openapi](contracts/openapi) |
| Servicios, bases MongoDB, RabbitMQ, Redis | sin puerto en el host | Para depurar, ver "Abrir puertos internos" más abajo |
| Grafana / Jaeger / Prometheus | http://localhost:3000 · http://localhost:16686 · http://localhost:9090 | Solo con el stack de observabilidad (ver abajo) |

Cada servicio tiene su propia base con su propio usuario (`readWrite` solo sobre ella): `identity_db`, `notification_db`, `customer_db`, `claims_db`, `quotation_db` y `policy_db`. Las claves por defecto sirven solo en local; se cambian en `Arquitectura-Clean/.env` (ver `.env.example`).

> Nota: el `docker-compose.yml` de la raíz del repositorio es un subconjunto antiguo (solo el monolito y su MongoDB). Para el stack completo usar siempre el de `Arquitectura-Clean/`.

## Levantar el stack completo

```bash
cd Arquitectura-Clean
docker compose up -d --build
docker compose ps
docker compose logs -f gateway policy-service notification-service
```

identity-service y customer-service arrancan con usuarios y clientes de demostración. Las tablas tarifarias, cotizaciones, pólizas y siniestros no: vienen de los scripts `services/*/migracion/migrar-*.sh`, que copian la base del monolito (requieren levantar antes el perfil `monolito`, ver abajo), o se crean por la API (`POST /api/tablas-tarifarias`, ADMIN). Si alguna proyección quedó vacía, se vuelve a poblar con los backfill (usuario ADMIN):

```bash
curl -X POST http://localhost:8080/api/clientes/eventos/reenvio  -H "Authorization: Bearer <token>"
curl -X POST http://localhost:8080/api/siniestros/eventos/reenvio -H "Authorization: Bearer <token>"
```

Probar sin enviar WhatsApp reales ni consultar JSON.pe:

```bash
WHATSAPP_BASE_URL=http://whatsapp-mock:8080 docker compose --profile whatsapp-mock up -d
JSONPE_BASE_URL=http://jsonpe-mock:8080 docker compose --profile jsonpe-mock up -d
```

### Abrir puertos internos (solo para depurar)

```bash
docker compose -f docker-compose.yml -f docker-compose.debug.yml up -d
# RabbitMQ: http://localhost:15672
```

### Observabilidad (Grafana, Prometheus, Loki, Jaeger)

```bash
docker compose -f docker-compose.yml -f ../infra/observability/docker-compose.observability.yml up -d
```

- **Grafana** (http://localhost:3000, usuario `admin`, clave `GRAFANA_ADMIN_PASSWORD` o `grafana-local`): tablero "Andina Seguros — Resumen" y, en Explore, los logs de todos los servicios. Para seguir una petición: `{service=~".+"} |= "<X-Correlation-Id>"`; desde cada log, el `traceId` abre la traza en Jaeger.
- **Jaeger** (http://localhost:16686): una emisión es una sola traza: gateway → policy-service → RabbitMQ → notification-service, claims-service y quotation-service.
- **Prometheus** (http://localhost:9090): métricas de los 7 servicios y de RabbitMQ (mensajes por cola), y alertas: circuito abierto, DLQ con mensajes (de cualquier servicio), Outbox atrasado, 5xx, servicio caído y notificaciones pausadas.

Detener sin borrar datos:

```bash
docker compose down
```

## Monolito retirado (reversa)

El monolito se retiró en el paso 6.10. Su base está respaldada en `respaldos/` (no versionado) y el volumen `andina_clean_mongo_data` se conserva. Para volver a levantarlo durante el periodo de seguridad:

```bash
docker compose --profile monolito up -d mongodb backend
# y, para devolverle una ruta, apuntar la URL del servicio en el gateway, por ejemplo:
POLICY_SERVICE_URL=http://backend:8080 docker compose up -d gateway
```

Las escrituras hechas después del corte están solo en los microservicios: antes de una reversa hay que copiarlas al monolito.

## Aislamiento

- Cada servicio ↔ su MongoDB: una red propia (`customer_data_network`, `claims_data_network`, `quotation_data_network`, `policy_data_network`, `andina_identity_data_network`, `andina_notification_data_network`). Ningún servicio llega a la base de otro.
- Servicios ↔ RabbitMQ: red `rabbitmq_network`. Gateway ↔ servicios: `andina_services_network`. Gateway ↔ Redis: `andina_gateway_network`.
- RabbitMQ tiene `hostname: rabbitmq`: al recrear el contenedor conserva colas y mensajes.
- Volúmenes: uno por base (`customer_mongo_data`, `claims_mongo_data`, `quotation_mongo_data`, `policy_mongo_data`, `andina_identity_mongo_data`, `andina_notification_mongo_data`), `rabbitmq_data` y, del monolito retirado, `andina_clean_mongo_data`.
