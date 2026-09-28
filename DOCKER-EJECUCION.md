# Ejecución Docker — API Gateway y microservicios

El stack local es: **frontend, API Gateway, identity-service, notification-service, customer-service, claims-service, quotation-service y policy-service, un MongoDB por servicio, RabbitMQ y Redis**, definidos en [docker-compose.yml](docker-compose.yml) en la raíz del repositorio. Todos los comandos de esta guía se ejecutan **desde la raíz**. El monolito se retiró del repositorio el 2026-09-28 (ver "Monolito retirado" al final).

Detalle de cada fase de la migración: [doc/5. Microservicios/](doc/5.%20Microservicios/) (`e_` fase 0, `f_` fase 1, `g_` fase 2, `i_` fase 3, `j_` fase 4, `l_` fase 5, `m_` fase 6, `n_` fase 7 y `q_` retiro del monolito).

## Puertos publicados al host

Solo el frontend y el gateway exponen puertos (paso 0.6 de la ruta). Todo lo demás vive en redes internas de Docker.

| Servicio | URL | Notas |
|---|---|---|
| **Frontend** | http://localhost:5173 | Único punto de entrada pensado para el usuario final |
| **API Gateway** | http://localhost:8080/api | Único punto de entrada de la API. Los contratos de cada servicio están en [contracts/openapi](contracts/openapi) |
| Servicios, bases MongoDB, RabbitMQ, Redis | sin puerto en el host | Para depurar, ver "Abrir puertos internos" más abajo |
| Grafana / Jaeger / Prometheus | http://localhost:3000 · http://localhost:16686 · http://localhost:9090 | Solo con el stack de observabilidad (ver abajo) |

Cada servicio tiene su propia base con su propio usuario (`readWrite` solo sobre ella): `identity_db`, `notification_db`, `customer_db`, `claims_db`, `quotation_db` y `policy_db`. Las claves por defecto sirven solo en local; se cambian en el `.env` de la raíz (plantilla: [.env.example](.env.example); `.env` nunca se versiona).

## Levantar el stack completo

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f gateway policy-service notification-service
```

Una instalación nueva no necesita el monolito (`APP_DEMO_DATA_ENABLED=true` en Compose; si los datos ya existen no se tocan):

- identity-service crea el usuario `admin` / `Admin123*` (ADMIN, sin MFA en una base nueva). Los demás usuarios de [doc/0. USUARIOS-DE-PRUEBA.md](doc/0.%20USUARIOS-DE-PRUEBA.md) (`admin2`, la cuenta CLIENTE y la AGENTE) están en el volumen de `identity_db` porque llegaron con la migración; en una base nueva se crean por la API (`POST /api/auth/register`: CLIENTE sin sesión, personal con un ADMIN).
- customer-service crea los clientes y vehículos demo.
- quotation-service crea las 3 tablas tarifarias demo (las mismas del monolito, con los mismos ids).

Con eso ya se puede cotizar, aceptar, emitir, registrar siniestros y renovar por la API. Si alguna proyección quedó vacía, se vuelve a poblar con los backfill (usuario ADMIN):

```bash
curl -X POST http://localhost:8080/api/clientes/eventos/reenvio  -H "Authorization: Bearer <token>"
curl -X POST http://localhost:8080/api/siniestros/eventos/reenvio -H "Authorization: Bearer <token>"
```

Probar sin enviar WhatsApp reales ni consultar JSON.pe:

```bash
WHATSAPP_BASE_URL=http://whatsapp-mock:8080 docker compose --profile whatsapp-mock up -d
JSONPE_BASE_URL=http://jsonpe-mock:8080 docker compose --profile jsonpe-mock up -d
```

Con HTTPS local (fase 7, perfil `tls`: https://localhost:8443 el frontend y https://localhost:8444 el gateway, con una CA local de Caddy):

```bash
FRONTEND_API_URL=https://localhost:8444/api FRONTEND_API_ORIGIN=https://localhost:8444 \
CORS_ALLOWED_ORIGINS=https://localhost:8443 docker compose --profile tls up -d --build
```

### Operación (fase 7)

Desde la raíz del repositorio; qué hacer con cada resultado está en la [guía de operación](doc/5.%20Microservicios/o_GUIA-OPERACION.md).

| Tarea | Comando |
|---|---|
| Reconciliar proyecciones con sus fuentes | `sh infra/operacion/reconciliar.sh` |
| Respaldar las 6 bases / probar la restauración | `sh infra/mongo/respaldar.sh` / `sh infra/mongo/probar-restauracion.sh respaldos/<fecha>` |
| Reprocesar una DLQ | `sh infra/rabbitmq/reprocesar-dlq.sh <cola>.dlq` |
| Pruebas de caos | `bash infra/operacion/caos.sh [caso...]` (con WhatsApp y JSON.pe simulados) |
| Prueba de carga | `docker run --rm -i --network gateway_network grafana/k6:0.54.0 run - < infra/carga/carga.js` |

### Abrir puertos internos (solo para depurar)

```bash
docker compose -f docker-compose.yml -f docker-compose.debug.yml up -d
# RabbitMQ: http://localhost:15672
```

### Observabilidad (Grafana, Prometheus, Loki, Jaeger)

```bash
docker compose -f docker-compose.yml -f infra/observability/docker-compose.observability.yml up -d
```

- **Grafana** (http://localhost:3000, usuario `admin`, clave `GRAFANA_ADMIN_PASSWORD` o `grafana-local`): tablero "Backend Seguros — Resumen" y, en Explore, los logs de todos los servicios. Para seguir una petición: `{service=~".+"} |= "<X-Correlation-Id>"`; desde cada log, el `traceId` abre la traza en Jaeger.
- **Jaeger** (http://localhost:16686): una emisión es una sola traza: gateway → policy-service → RabbitMQ → notification-service, claims-service y quotation-service.
- **Prometheus** (http://localhost:9090): métricas de los 7 servicios y de RabbitMQ (mensajes por cola), readiness de cada servicio (blackbox) y alertas: servicio caído o no listo, circuito abierto, DLQ con mensajes (de cualquier servicio), Outbox atrasado, 5xx y notificaciones pausadas.
- **Alertmanager** (http://localhost:9093): envía las alertas por correo a ramirezlisset361@gmail.com (Gmail), con el cuerpo predeterminado. Requiere la contraseña de aplicación de Google en el `.env` de la raíz como `ALERTMANAGER_SMTP_PASSWORD`; configuración en `infra/observability/alertmanager/alertmanager.yml`. Detalle en la [guía de operación](doc/5.%20Microservicios/o_GUIA-OPERACION.md), sección 3.1.

Detener sin borrar datos:

```bash
docker compose down
```

## Monolito retirado

El monolito (`Arquitectura-Clean`) dejó de recibir tráfico en el corte (2026-09-27), se sacó del Compose en el paso 6.10 y **se retiró del repositorio el 2026-09-28** ([q_RETIRO-DEL-MONOLITO.md](doc/5.%20Microservicios/q_RETIRO-DEL-MONOLITO.md)). Ya no hay reversa automática; lo que queda es:

| Qué | Dónde |
|---|---|
| Código del monolito (último estado) | Etiqueta de git `monolito-final`: `git checkout monolito-final` o `git worktree add ../monolito monolito-final` |
| Datos de su MongoDB | `respaldos/monolito-andina_seguros_clean-2026-09-27.archive.gz` (no versionado); se restaura con `mongorestore --archive --gzip` en un MongoDB temporal |
| Scripts de migración | `services/*/migracion/migrar-*.sh`, marcados como históricos (leían la base del monolito) |

## Aislamiento

- Cada servicio ↔ su MongoDB: una red propia (`customer_data_network`, `claims_data_network`, `quotation_data_network`, `policy_data_network`, `identity_data_network`, `notification_data_network`). Ningún servicio llega a la base de otro.
- Servicios ↔ RabbitMQ: red `rabbitmq_network`. Gateway ↔ servicios: `services_network`. Gateway ↔ Redis: `gateway_network`.
- RabbitMQ tiene `hostname: rabbitmq`: al recrear el contenedor conserva colas y mensajes.
- Volúmenes: uno por base (`customer_mongo_data`, `claims_mongo_data`, `quotation_mongo_data`, `policy_mongo_data`, `andina_identity_mongo_data`, `andina_notification_mongo_data`), `andina-clean_rabbitmq_data` y los del stack de observabilidad (`andina-clean_*_data`). El volumen del monolito (`andina_clean_mongo_data`) se borró al retirarlo, tras comprobar el respaldo.
- El proyecto de Compose se llama `backend-seguros` (`name:` en `docker-compose.yml`); los contenedores y las redes no llevan el nombre de la empresa. Los volúmenes creados antes conservan su nombre (`andina_*`, `andina-clean_*`) porque renombrar un volumen obliga a copiar sus datos; en el caso de `andina_identity_keys` (clave de firma del JWT), además invalidaría todas las sesiones.
