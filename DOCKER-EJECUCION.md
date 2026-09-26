# Ejecución Docker — API Gateway, backend Clean, identity-service y notification-service

Las arquitecturas Onion y Hexagonal se retiraron del compose (ver commit `chore(docker): elimina servicios Onion y Hexagonal del compose raíz`). Hoy el stack local es: **frontend, API Gateway, backend Clean, identity-service, notification-service, un MongoDB por servicio (backend, identity, notification), RabbitMQ y Redis**, definidos en [Arquitectura-Clean/docker-compose.yml](Arquitectura-Clean/docker-compose.yml).

Detalle de cada fase de la migración a microservicios:

- Fase 0, API Gateway: [doc/5. Microservicios/e_IMPLEMENTACION-API-GATEWAY-FASE0.md](doc/5.%20Microservicios/e_IMPLEMENTACION-API-GATEWAY-FASE0.md).
- Fase 1, notification-service: [doc/5. Microservicios/f_IMPLEMENTACION-NOTIFICATION-SERVICE-FASE1.md](doc/5.%20Microservicios/f_IMPLEMENTACION-NOTIFICATION-SERVICE-FASE1.md).
- Fase 2, identity-service: [doc/5. Microservicios/g_IMPLEMENTACION-IDENTITY-SERVICE-FASE2.md](doc/5.%20Microservicios/g_IMPLEMENTACION-IDENTITY-SERVICE-FASE2.md).

## Puertos publicados al host

| Servicio | URL | Notas |
|---|---|---|
| **Frontend** | http://localhost:5173 | Único punto de entrada pensado para el usuario final |
| **API Gateway** | http://localhost:8080/api | Único punto de entrada de la API (patrón Strangler Fig); el frontend ya apunta aquí |
| Backend Clean (directo) | http://localhost:8083/api · Swagger en `/swagger-ui.html` | Se mantiene publicado por continuidad (pruebas manuales) mientras se termina de migrar todo el tráfico al gateway. Desde la fase 2 ya no tiene login: los tokens se piden por el gateway (`/api/auth/login`) |
| MongoDB del backend | localhost:27020 | Base `andina_seguros_clean`. Es un replica set de un nodo (`rs0`): desde el host usar `mongodb://localhost:27020/?directConnection=true` |
| RabbitMQ (AMQP / UI) | localhost:5672 / localhost:15672 | — |
| notification-service | sin puerto en el host | Solo Actuator interno (`/actuator/health`, `/actuator/prometheus`) |
| MongoDB de notification | sin puerto en el host | Base `notification_db`, usuario `notification` |
| identity-service | sin puerto en el host | Detrás del gateway (`/api/auth/**`, `/api/mfa/**`). Escalable: `docker compose up -d --scale identity-service=2` |
| MongoDB de identity | sin puerto en el host | Base `identity_db`, usuario `identity` |
| Redis | sin puerto en el host | Rate limiting del gateway (base 0) y estado efímero de identity-service (base 1) |

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
