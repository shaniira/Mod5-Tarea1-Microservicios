# Andina Seguros — migración de monolito a microservicios

Sistema de seguros vehiculares (cotizaciones, pólizas, siniestros, renovaciones) que se está migrando de un monolito Clean Architecture a microservicios con el patrón Strangler Fig.




## Documentos de referencia (leer antes de cambiar la arquitectura)

- `doc/5. Microservicios/c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md`: arquitectura objetivo, contratos de eventos, resiliencia y observabilidad.
- `doc/5. Microservicios/d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md`: fases con sus pasos y criterios de salida (los cumplidos están marcados).
- `doc/5. Microservicios/f_…` (fase 1), `g_…` (fase 2), `h_CIERRE-PENDIENTES.md`, `i_…` (fase 3), `j_…` (fase 4), `l_…` (fase 5), `m_…` (fase 6 y corte): qué se implementó, decisiones y evidencias.
- `doc/5. Microservicios/k_IMPACTO-EN-EL-MONOLITO.md`: qué se cambió en el monolito en cada fase y commit, y qué haría falta para cada corte.
- `contracts/`: esquemas de eventos y OpenAPI. Todo evento nuevo se define aquí primero.
- `doc/0. STANDAR-COMMITS.md`: estándar de commits.

## Estado actual (2026-09-27)

| Fase                                                       | Estado       | Responsable |
| ---------------------------------------------------------- | ------------ | ----------- |
| 0. Preparación y seguridad                                | ✅ Cerrada   | —          |
| 1. notification-service                                    | ✅ Cerrada   | —          |
| 2. identity-service                                        | ✅ Cerrada   | —          |
| 3. customer-service                                        | ✅ Cerrada (corte 2026-09-27) | Shanira |
| 4. claims-service                                          | ✅ Cerrada (corte 2026-09-27) | Shanira |
| 5. quotation-service                                       | ✅ Cerrada (corte 2026-09-27) | Shanira |
| 6. policy-service (Saga de emisión, se apaga el monolito) | 🟡 Corte hecho; falta retirar el monolito (6.10, 6.11) y observabilidad | Shanira |
| 7. Endurecimiento                                          | ⏳ Pendiente | —          |

Trabajo en paralelo: cada fase en su propia rama (por ejemplo `feat/fase5-quotation`), con PRs pequeños.

**Monolito congelado (desde 2026-09-27):** no se modifica el código de `Arquitectura-Clean` (`src`, `pom.xml`, `Dockerfile`). Desde el corte (2026-09-27) el gateway envía todo el negocio a customer, claims, quotation y policy, que publican sus eventos (`*_EVENTS_PUBLISH_ENABLED=true`); el monolito sigue en el Compose sin tráfico, como reversa (apuntar las `*_SERVICE_URL` del gateway a `http://backend:8080`). En `docker-compose.yml` y `.env.example` solo se agregan entradas. Los nombres nuevos (imágenes, contenedores, volúmenes, redes, documentos) van sin el prefijo de la empresa.

### Acuerdos entre fases

- ✅ **`vehicle.registered.v1`**: definido en `contracts/events/` y publicado por customer-service (fase 3). `customer.*` lleva ahora `birthDate` (opcional) para la edad del conductor. Con el monolito congelado, el monolito no lo publica.
- ✅ **`quote.accepted.v1`**: publicado por quotation-service y consumido por policy-service (`accepted_quotes`).
- ✅ **Emisión de póliza**: cotizaciones y pólizas se cortaron juntas; la saga (quote.accepted → policy.issued / policy.issuance-rejected) ya no pasa por el monolito.
- ✅ **claims ↔ policy:** policy-service consume `claim.*` (`claim_ref`) y publica `policy.renewed`; `policy.expired/cancelled` tienen contrato pero ninguna operación los produce todavía.
- Archivos que tocan varias fases (hacer cambios chicos e integrar seguido): `gateway/src/main/resources/application.yml` (rutas), `Arquitectura-Clean/docker-compose.yml`, `k8s/`, `contracts/`, `UseCaseConfig` del backend.

## Estructura

| Carpeta                            | Qué es                                                                                                             |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| `Arquitectura-Clean/`            | Monolito (Spring Boot 3.3, Java 21, Clean Architecture) y el`docker-compose.yml` del stack completo               |
| `gateway/`                       | API Gateway: Spring Cloud Gateway, Spring Security OAuth2 Resource Server (JWT RS256 por JWKS), Redis, Resilience4j |
| `services/identity-service/`     | Usuarios, login (contraseña, Google, Facebook), MFA, emisión de JWT RS256, revocación en Redis                   |
| `services/notification-service/` | WhatsApp de póliza emitida; proyección`customer_contacts`; circuit breaker que pausa el listener                |
| `services/customer-service/`     | Clientes, vehículos, consulta de placas (JSON.pe con caché Redis y resiliencia); publica `customer.*` y `vehicle.registered` |
| `services/claims-service/`       | Siniestros; proyección `policy_ref` (de `policy.issued`); publica `claim.*`                                          |
| `services/policy-service/`       | Pólizas, saga de emisión (índice único por cotización, compensación), renovaciones; proyecciones `accepted_quotes` y `claim_ref` |
| `services/quotation-service/`    | Tablas tarifarias, motor de tarificación, cotizaciones; proyecciones `customer_ref` y `vehicle_ref` con lectura de refuerzo a customer-service; publica `quote.accepted` |
| `frontend/`                      | Vue 3.5 + TypeScript + Vite + Pinia; llama solo al gateway                                                          |
| `contracts/`                     | Esquemas JSON de eventos y OpenAPI                                                                                  |
| `infra/mongo/`                   | Arranque de MongoDB con replica set y autenticación (Compose y Kubernetes)                                         |
| `infra/observability/`           | OTel Collector, Jaeger, Prometheus (alertas), Loki, Promtail, Grafana                                               |
| `k8s/`                           | Manifiestos de Kubernetes (ver`k8s/README.md`)                                                                    |

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





## Cómo retomar el trabajo

Antes de programar:

1. Ejecuta `git status` y conserva todos los cambios existentes.
2. Lee los documentos de la fase correspondiente y revisa sus criterios de salida.
3. Comprueba qué está implementado y qué falta; no supongas que el estado de esta guía sigue actualizado.
4. Si hay decisiones pendientes que involucran otras fases, identifícalas antes de cambiar contratos o archivos compartidos.

Al terminar:

- Actualiza el estado de esta guía y la documentación de la fase.
- Ejecuta las pruebas disponibles y resume cuáles pasaron y cuáles no.
- No hagas commits, no cambies de rama y no reviertas cambios existentes, salvo que te lo pida.

## Siguiente trabajo

Las fases 3 a 6 están implementadas y el corte está hecho (ver `m_…`, sección 7). Lo que sigue:

1. **Paso 6.10:** respaldar `andina_clean_mongo_data` y retirar `backend` del Compose (y su ruta de reserva del gateway).
2. **Paso 6.11:** quitar los enlaces a `andina.insurance.events` en notification, claims y quotation cuando nadie publique ahí.
3. **Observabilidad:** sumar customer, claims, quotation y policy a Prometheus y al Compose de observabilidad (criterio 3 de la fase 6).
4. **Kubernetes:** manifiestos de los 4 servicios nuevos y del gateway validados con `--dry-run=server` (`k8s/70`–`98`); falta cargar las imágenes y aplicarlos.
5. **Fase 7:** caos, carga, reconciliación, cola de auditoría sin consumidor.

Para probar localmente: `--profile jsonpe-mock` con `JSONPE_BASE_URL=http://jsonpe-mock:8080` simula JSON.pe, y `--profile whatsapp-mock` con `WHATSAPP_BASE_URL=http://whatsapp-mock:8080` simula WhatsApp.
