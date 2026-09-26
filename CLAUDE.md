# Andina Seguros — migración de monolito a microservicios

Sistema de seguros vehiculares (cotizaciones, pólizas, siniestros, renovaciones) que se está migrando de un monolito Clean Architecture a microservicios con el patrón Strangler Fig.

## Documentos de referencia (leer antes de cambiar la arquitectura)

- `doc/5. Microservicios/c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md`: arquitectura objetivo, contratos de eventos, resiliencia y observabilidad.
- `doc/5. Microservicios/d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md`: fases con sus pasos y criterios de salida (los cumplidos están marcados).
- `doc/5. Microservicios/f_…` (fase 1), `g_…` (fase 2), `h_CIERRE-PENDIENTES.md`: qué se implementó, decisiones y evidencias.
- `contracts/`: esquemas de eventos y OpenAPI. Todo evento nuevo se define aquí primero.
- `doc/0. STANDAR-COMMITS.md`: estándar de commits.

## Estado actual (2026-09-26)

| Fase | Estado | Responsable |
|---|---|---|
| 0. Preparación y seguridad | ✅ Cerrada | — |
| 1. notification-service | ✅ Cerrada | — |
| 2. identity-service | ✅ Cerrada | — |
| 3. customer-service | ⏳ Pendiente | compañero |
| 4. claims-service | ⏳ Pendiente | compañero |
| 5. quotation-service | ⏳ Siguiente | Shanira |
| 6. policy-service (Saga de emisión, se apaga el monolito) | ⏳ Pendiente | compañero |
| 7. Endurecimiento | ⏳ Pendiente | — |

Trabajo en paralelo: cada fase en su propia rama (por ejemplo `feat/fase5-quotation`), con PRs pequeños.

### Acuerdos pendientes entre fases (resolver antes de programar la fase 5)

- **`vehicle.registered.v1`**: lo publicará customer-service (fase 3); quotation lo consume para su copia `vehicle_ref`. Definir el esquema juntos en `contracts/events/`. Mientras tanto, el monolito puede publicarlo.
- **`quote.accepted.v1`**: lo publica quotation (fase 5) y lo consume policy-service (fase 6). Definir el esquema juntos.
- **Emisión de póliza en la transición**: hoy `EmitirPolizaUseCase` del monolito lee `cotizaciones` directo. Si quotation sale antes que policy-service, el monolito necesita una copia `accepted_quotes` alimentada por `quote.accepted.v1`.
- Archivos que tocan varias fases (hacer cambios chicos e integrar seguido): `gateway/src/main/resources/application.yml` (rutas), `Arquitectura-Clean/docker-compose.yml`, `k8s/`, `contracts/`, `UseCaseConfig` del backend.

## Estructura

| Carpeta | Qué es |
|---|---|
| `Arquitectura-Clean/` | Monolito (Spring Boot 3.3, Java 21, Clean Architecture) y el `docker-compose.yml` del stack completo |
| `gateway/` | API Gateway: Spring Cloud Gateway, Spring Security OAuth2 Resource Server (JWT RS256 por JWKS), Redis, Resilience4j |
| `services/identity-service/` | Usuarios, login (contraseña, Google, Facebook), MFA, emisión de JWT RS256, revocación en Redis |
| `services/notification-service/` | WhatsApp de póliza emitida; proyección `customer_contacts`; circuit breaker que pausa el listener |
| `frontend/` | Vue 3.5 + TypeScript + Vite + Pinia; llama solo al gateway |
| `contracts/` | Esquemas JSON de eventos y OpenAPI |
| `infra/mongo/` | Arranque de MongoDB con replica set y autenticación (Compose y Kubernetes) |
| `infra/observability/` | OTel Collector, Jaeger, Prometheus (alertas), Loki, Promtail, Grafana |
| `k8s/` | Manifiestos de Kubernetes (ver `k8s/README.md`) |

## Reglas del proyecto

- Cada servicio: carpetas `entities`, `usecases`, `interfaceadapters`, `frameworksdrivers` y prueba ArchUnit (plantilla en la sección 3 de la ruta).
- **Database per service**: ningún servicio lee la base de otro. Los datos ajenos se copian con eventos (proyecciones con `aggregateVersion` para descartar eventos viejos).
- Publicar eventos siempre con **Outbox**; consumir con **inbox/idempotencia**; cada consumidor es dueño de sus colas y su DLQ.
- Exchange nuevo `andina.events`; `policy.issued.v1` sigue en `andina.insurance.events` (heredado). Las colas escuchan ambos durante la transición.
- Seguridad: cada servicio valida el JWT (RS256, JWKS de identity) y aplica rol y propietario (`customerId` del token). Reglas por rol del backend en `interfaceadapters/in/rest/security/Roles.java`.
- Solo el gateway (8080) y el frontend (5173) publican puertos. Secretos solo por variables de entorno (`.env`, nunca versionado).
- Comentarios y documentación en español.

## Commits

Conventional Commits en español: `tipo(scope): descripción en presente`, **asunto corto (≤ 50 caracteres)**, sin punto final, detalle en el cuerpo con 2-4 viñetas. Un cambio principal por commit.

## Cómo levantar y probar

```bash
cd Arquitectura-Clean
docker compose up -d --build                                    # stack completo
docker compose -f docker-compose.yml -f docker-compose.debug.yml up -d   # abre puertos internos para depurar
docker compose -f docker-compose.yml -f ../infra/observability/docker-compose.observability.yml up -d   # + Grafana :3000, Jaeger :16686, Prometheus :9090
WHATSAPP_BASE_URL=http://whatsapp-mock:8080 docker compose --profile whatsapp-mock up -d   # WhatsApp simulado
```

- Primera vez: `sh services/identity-service/migracion/migrar-usuarios.sh` y el backfill `POST /api/clientes/eventos/reenvio` (ADMIN). Detalle en `DOCKER-EJECUCION.md`.
- Pruebas: Maven no está instalado en el host; se usan en Docker, por ejemplo:
  `docker run --rm -v "<ruta>:/app" -v "$HOME/.m2:/root/.m2" -w /app maven:3.9.9-eclipse-temurin-21 mvn -q test`. Las imágenes también corren las pruebas al construirse.
- Usuarios de prueba: `doc/0. USUARIOS-DE-PRUEBA.md` (`admin2`/`admin2` es ADMIN sin MFA; `admin` tiene MFA). El login tiene rate limit de 1 por segundo (ráfaga de 5).
- En Windows con Git Bash, anteponer `MSYS_NO_PATHCONV=1` a comandos con rutas que empiezan con `/`.

## Pendientes fuera del código

- Rotar en JSON.pe los tokens que estuvieron versionados (lo hace el dueño de la cuenta).
- Alertmanager para enviar alertas por correo o chat.
- Probar el frontend en el navegador con cuentas reales de Google y Facebook.
