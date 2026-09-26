# Ejecución Docker — backend Clean + API Gateway

Las arquitecturas Onion y Hexagonal se retiraron del compose (ver commit `chore(docker): elimina servicios Onion y Hexagonal del compose raíz`). Hoy el stack local es: **frontend, API Gateway, backend Clean, notification consumer, MongoDB, RabbitMQ y Redis**, definidos en [Arquitectura-Clean/docker-compose.yml](Arquitectura-Clean/docker-compose.yml).

Detalle de la incorporación del Gateway (fase 0 de la migración a microservicios): [doc/5. Microservicios/e_IMPLEMENTACION-API-GATEWAY-FASE0.md](doc/5.%20Microservicios/e_IMPLEMENTACION-API-GATEWAY-FASE0.md).

## Puertos publicados al host

| Servicio | URL | Notas |
|---|---|---|
| **Frontend** | http://localhost:5173 | Único punto de entrada pensado para el usuario final |
| **API Gateway** | http://localhost:8080/api | Único punto de entrada de la API (patrón Strangler Fig); el frontend ya apunta aquí |
| Backend Clean (directo) | http://localhost:8083/api · Swagger en `/swagger-ui.html` | Se mantiene publicado por continuidad (pruebas manuales, colección Bruno) mientras se termina de migrar todo el tráfico al gateway; no debe usarse desde el frontend |
| MongoDB | localhost:27020 | Base `andina_seguros_clean` |
| RabbitMQ (AMQP / UI) | localhost:5672 / localhost:15672 | — |
| Redis | sin puerto en el host | Solo lo consume el gateway (rate limiting), dentro de `andina_gateway_network` |

> Nota: el `docker-compose.yml` de la raíz del repositorio es un subconjunto reducido (solo `clean-mongodb` + `clean-backend`, sin gateway) pensado para levantar el backend a solas; para el stack completo usar siempre el de `Arquitectura-Clean/`.

## Levantar el stack completo

```bash
cd Arquitectura-Clean
docker compose up -d --build
docker compose ps
```

Ver logs (incluye el gateway):

```bash
docker compose logs -f gateway backend consumer
```

Detener sin borrar datos:

```bash
docker compose down
```

## Aislamiento

- Backend ↔ MongoDB: red `andina_clean_network`.
- Backend/Consumer ↔ RabbitMQ: red `rabbitmq_network`.
- Gateway ↔ Backend: `andina_clean_network`. Gateway ↔ Redis: `andina_gateway_network`.
- Volúmenes: `andina_clean_mongo_data`, `rabbitmq_data`.

No ejecutar a la vez el Compose de la raíz y el de `Arquitectura-Clean/`: ambos usan el mismo nombre de red y de contenedor para Mongo y backend.
