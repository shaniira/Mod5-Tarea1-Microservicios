# Andina Seguros — Cierre de pendientes (fase 0 y fases 1-2)

Este documento registra el cierre de los pendientes que habían quedado después de las fases 1 y 2, y de lo que faltaba de la fase 0 ([ruta, sección 4](d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md#4-fase-0--preparación-y-seguridad-base)). Todo se verificó el 2026-09-26 contra el stack de Docker Compose y, en el caso de MongoDB, también en Kubernetes (Minikube).

---

## 1. Qué se cerró

| Pendiente | Qué se hizo | Commit |
|---|---|---|
| **S1** Registro con elección de rol (paso 0.2) | El registro público solo crea cuentas CLIENTE. Un rol de personal exige un ADMIN autenticado; si no, `403 ROL_NO_PERMITIDO` | `502526c` fix(auth) |
| **Usuario desactivado conserva acceso** y **logout sin efecto (S8)** | Lista de revocación en Redis con TTL igual a la vida del token. `POST /api/auth/logout` revoca el token; `PATCH /api/auth/usuarios/{u}/estado` corta las sesiones al desactivar. La consultan identity-service y el gateway | `e78781f` feat(auth) |
| **S2** Control de acceso (paso 0.3) | Reglas por rol en todos los endpoints del backend, alineadas con lo que el frontend muestra a cada rol. Un CLIENTE solo accede a lo suyo (cliente, vehículos, cotización, póliza, siniestros, historial), comparado con el `customerId` del token | `07d884f` feat(security) |
| **Gateway con Spring Security OAuth2 Resource Server** | El filtro propio se reemplazó por Spring Security WebFlux (resource server con JWKS), que además rechaza tokens revocados. El 401 lleva CORS y `correlationId`; los encabezados `X-User-*` enviados por el cliente se descartan | `bcf235e` feat(gateway) |
| Datos demo y Swagger solo en desarrollo (pasos 0.5 y 0.11) | Apagados por defecto; Compose los activa en local. El login ya no sugiere la contraseña del usuario demo | `08f6660` chore(config) |
| **Gateway con IP vieja tras recrear un contenedor** (encontrado en esta etapa) | Cache DNS de 10 s y conexiones con vida limitada: antes el gateway respondía 503 hasta reiniciarse | `d2c524b` fix(gateway) |
| **S4/S5** MongoDB y RabbitMQ (paso 0.6) y replica set (0.7) | MongoDB del backend con usuario propio, keyFile y replica set, sirve también para el volumen con datos previos (`infra/mongo/arranque.sh`). MongoDB, RabbitMQ y el backend sin puertos en el host; `docker-compose.debug.yml` los abre solo para depurar | `7257098` chore(docker) |
| MongoDB en Kubernetes | Replica set con autenticación y un usuario por servicio (mismos scripts); Service headless | `bd9b99f` feat(k8s) |
| notification-service en Kubernetes | Deployment con probes, rolling update y apagado ordenado | `c40f458` feat(k8s) |
| **Observabilidad** (paso 0.9) | `infra/observability`: OpenTelemetry Collector, Jaeger, Prometheus con alertas, Loki, Promtail y Grafana (tablero y fuentes enlazadas) | `35bb9ba` feat(infra) |
| Trazas y logs JSON en backend y gateway | Micrometer Tracing con exportación OTLP y logs JSON. El Outbox guarda el `traceparent`, así la traza sigue a través de RabbitMQ | `cc2024a`, `3ba32be` |
| CI (paso 0.14) | Pipelines del backend y del gateway (este con Redis como servicio) | `04f32cb` chore(ci) |

## 2. Verificación

| Prueba | Resultado |
|---|---|
| Registro público pidiendo ADMIN | 403; sin rol crea un CLIENTE; un ADMIN autenticado crea un ACTUARIO (201) |
| CLIENTE: lista de clientes / su cliente / otro cliente | 403 / 200 / 403 |
| CLIENTE: su póliza / póliza de otro / siniestros de otro | 200 / 403 / 403 |
| ACTUARIO: consultar pólizas / crear cotización | 200 / 403 |
| Mi cuenta con token ADMIN | 403 (es solo para CLIENTE) |
| Logout | El mismo token da 401 en el gateway y en identity-service |
| Usuario desactivado por un ADMIN | Sus sesiones abiertas dan 401 al instante y no puede entrar; al reactivarlo vuelve a entrar |
| Gateway sin token | 401 con cuerpo JSON, `correlationId` y cabeceras CORS; firma alterada = 401; una sola `Access-Control-Allow-Origin` |
| **Total del script de seguridad** | **27 de 27 comprobaciones OK** |
| MongoDB sin credenciales | `requires authentication`; el usuario `andina` no puede leer otras bases; datos previos intactos (9 clientes, 11 pólizas); replica set `rs0` PRIMARY; el backend ya no avisa "sin transacción" |
| MongoDB en Kubernetes (Minikube, namespace temporal) | Pod listo, `rs0` PRIMARY con el nombre del Service headless, autenticación obligatoria, transacción de `identity` en su base OK y rechazo en base ajena |
| Gateway tras recrear el backend (nueva IP) | 6 de 6 peticiones 200 sin reiniciar el gateway |
| Prometheus | 5 de 5 objetivos `up`; 6 reglas de alerta cargadas |
| Traza distribuida (Jaeger) | Una sola traza: gateway → `andina-backend` (`POST /api/polizas`) → RabbitMQ → notification-service; el alta de cliente continúa en identity-service y notification-service |
| Logs por `correlationId` (Loki vía Grafana) | Aparecen las líneas del gateway y de notification-service; estas llevan el mismo `traceId` que la traza de Jaeger |
| Pruebas automáticas | Backend 47, identity-service 59, notification-service 27, gateway 1 (con Redis) |

## 3. Alineación con el stack objetivo

| Área | Estado |
|---|---|
| Frontend (Vue 3.5, TS, Vite, Pinia, Router, Axios, Nginx) | ✅ |
| API Gateway (Spring Cloud Gateway, WebFlux, Spring Security OAuth2 Resource Server, JWT, Redis, Resilience4j, Actuator, Micrometer, Prometheus) | ✅ (Bulkhead: los límites de concurrencia hacia proveedores están en identity-service, donde ocurren las llamadas externas) |
| Microservicios existentes (identity, notification) | ✅ Java 21, Spring Boot, Clean Architecture, REST + OpenAPI (identity), Spring Security, MongoDB, RabbitMQ, Resilience4j, Actuator |
| customer, quotation, policy, claims | ⏳ Siguen en el monolito: son las fases 3 a 6 de la ruta |
| Datos (database per service) | ✅ para backend, identity y notification; las demás bases nacen con sus servicios |
| Mensajería (RabbitMQ, eventos, idempotencia, DLQ, Outbox) | ✅ · Saga: ⏳ fase 6 (emisión de póliza entre quotation y policy) |
| Kubernetes (Deployment, Service, Ingress NGINX, ConfigMap, Secret, DNS, HPA, probes, Rolling Update) | ✅ para gateway, backend, identity, notification, MongoDB, RabbitMQ y Redis |
| Resiliencia (circuit breaker, retry con backoff, timeout, bulkhead, health checks, idempotencia) | ✅ |
| Observabilidad (OpenTelemetry, Prometheus, Grafana, Loki, Jaeger, correlation ID, logs centralizados, métricas, trazas) | ✅ en Docker Compose · En Kubernetes los Pods ya exponen métricas (anotaciones) y aceptan `MANAGEMENT_OTLP_TRACING_ENDPOINT`; desplegar el stack en el clúster queda para la fase 7 |

## 4. Impacto en el monolito

Detalle por commit en [k_IMPACTO-EN-EL-MONOLITO.md](k_IMPACTO-EN-EL-MONOLITO.md#2-fase-0--preparación-y-seguridad).

| Commit | Cambio en `Arquitectura-Clean` |
|---|---|
| `07d884f` | Reglas por rol en todos los controladores (`Roles.java`) y control de propietario (`AccesoRecursos`) |
| `08f6660` | Datos demo y Swagger solo en desarrollo; el login ya no sugiere la contraseña demo |
| `7257098` | Solo Compose: MongoDB con usuario propio, keyFile y replica set; sin puertos en el host |
| `cc2024a` | Trazas OTLP, logs JSON y `traceparent` guardado en el Outbox |

Ninguno de estos commits sacó funcionalidad del monolito: son cambios de seguridad, configuración y observabilidad.

## 5. Lo que no depende del código

- **Tokens de JSON.pe que estuvieron versionados:** hay que rotarlos en el panel de JSON.pe (solo puede hacerlo el dueño de la cuenta).
- **Alertas por correo o chat:** las reglas están en Prometheus y se ven en Grafana; para enviar avisos falta configurar un Alertmanager con el canal del equipo.
- **Frontend en el navegador:** se verificaron el API y las cabeceras CORS con el origen del frontend; no se hizo una prueba manual en el navegador.
