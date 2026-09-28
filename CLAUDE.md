# Andina Seguros — migración de monolito a microservicios

Sistema de seguros vehiculares (cotizaciones, pólizas, siniestros, renovaciones) que se está migrando de un monolito Clean Architecture a microservicios con el patrón Strangler Fig.




## Documentos de referencia (leer antes de cambiar la arquitectura)

- `doc/5. Microservicios/c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md`: arquitectura objetivo, contratos de eventos, resiliencia y observabilidad.
- `doc/5. Microservicios/d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md`: fases con sus pasos y criterios de salida (los cumplidos están marcados).
- `doc/5. Microservicios/f_…` (fase 1), `g_…` (fase 2), `h_CIERRE-PENDIENTES.md`, `i_…` (fase 3), `j_…` (fase 4), `l_…` (fase 5), `m_…` (fase 6, corte y retiro del monolito): qué se implementó, decisiones y evidencias.
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
| 6. policy-service (Saga de emisión, se apaga el monolito) | ✅ Cerrada (monolito retirado 2026-09-27) | Shanira |
| 7. Endurecimiento                                          | ⏳ Pendiente | —          |

Trabajo en paralelo: cada fase en su propia rama (por ejemplo `feat/fase5-quotation`), con PRs pequeños.

**Monolito retirado (2026-09-27):** el código de `Arquitectura-Clean` (`src`, `pom.xml`, `Dockerfile`) queda archivado sin cambios. El gateway envía todo el negocio a customer, claims, quotation y policy, que publican sus eventos (`*_EVENTS_PUBLISH_ENABLED=true`). `backend` y su MongoDB solo arrancan con `--profile monolito` (reversa durante el periodo de seguridad); su base está respaldada en `respaldos/` (no versionado) y el volumen `andina_clean_mongo_data` se conserva. Los nombres nuevos (imágenes, contenedores, volúmenes, redes, documentos) van sin el prefijo de la empresa.

### Acuerdos entre fases

- ✅ **`vehicle.registered.v1`**: definido en `contracts/events/` y publicado por customer-service (fase 3). `customer.*` lleva ahora `birthDate` (opcional) para la edad del conductor. Con el monolito congelado, el monolito no lo publica.
- ✅ **`quote.accepted.v1`**: publicado por quotation-service y consumido por policy-service (`accepted_quotes`).
- ✅ **Emisión de póliza**: cotizaciones y pólizas se cortaron juntas; la saga (quote.accepted → policy.issued / policy.issuance-rejected) ya no pasa por el monolito.
- ✅ **claims ↔ policy:** policy-service consume `claim.*` (`claim_ref`) y publica `policy.renewed`; `policy.expired/cancelled` tienen contrato pero ninguna operación los produce todavía.
- Archivos que tocan varios servicios (hacer cambios chicos e integrar seguido): `gateway/src/main/resources/application.yml` (rutas), `Arquitectura-Clean/docker-compose.yml`, `infra/observability/`, `k8s/`, `contracts/`.

## Estructura

| Carpeta                            | Qué es                                                                                                             |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| `Arquitectura-Clean/`            | Monolito archivado (perfil `monolito`) y el `docker-compose.yml` del stack completo                               |
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
| `k8s/`                           | Manifiestos de Kubernetes (ver `k8s/README.md`); los del monolito, en `k8s/archivo-monolito/`                     |
| `respaldos/`                     | Respaldos de bases (no versionado): la del monolito tomada al retirarlo                                            |

## Reglas del proyecto

- Cada servicio: carpetas `entities`, `usecases`, `interfaceadapters`, `frameworksdrivers` y prueba ArchUnit (plantilla en la sección 3 de la ruta).
- **Database per service**: ningún servicio lee la base de otro. Los datos ajenos se copian con eventos (proyecciones con `aggregateVersion` para descartar eventos viejos).
- Publicar eventos siempre con **Outbox**; consumir con **inbox/idempotencia**; cada consumidor es dueño de sus colas y su DLQ.
- Todos los eventos van por `andina.events` (DLX `andina.events.dlx`). El heredado `andina.insurance.events` se retiró en el paso 6.11; solo queda `andina.insurance.events.dlx` como DLX de la cola `andina.policy.notification.queue` (sus argumentos no se pueden cambiar sin recrearla).
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

- Datos: identity y customer arrancan con datos demo; el resto sale de `services/*/migracion/migrar-*.sh` (leen la base del monolito: levantar antes `--profile monolito`). Backfill de proyecciones: `POST /api/clientes/eventos/reenvio` y `POST /api/siniestros/eventos/reenvio` (ADMIN). Detalle en `DOCKER-EJECUCION.md`.
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

Las fases 0 a 6 están cerradas: el monolito se retiró (ver `m_…`, secciones 7 y 9). Lo que sigue es la **fase 7** (ruta, sección 11), con estos puntos ya identificados:

1. **Kubernetes:** manifiestos de los servicios nuevos y del gateway validados con `--dry-run=server` (`k8s/70`–`98`); falta cargar las imágenes y aplicarlos. El clúster kind local todavía corre el despliegue de la fase 0 (con backend) y sobrecarga el equipo. RabbitMQ usa `emptyDir` (pierde mensajes al reiniciar).
2. **Auditoría:** `andina.policy.audit.queue` ya no existe en el nodo actual; sus 6 mensajes antiguos quedaron en el directorio del nodo anterior de RabbitMQ. Definir un consumidor de auditoría sobre `andina.events`.
3. **Renovación y siniestros recientes:** la sincronización de `claim_ref` no ve eventos en el Outbox de claims ni mensajes sin confirmar (`m_…`, sección 5).
4. **Ids que no son UUID** responden 500 (como el monolito); deberían dar 400.
5. **DLX heredada** `andina.insurance.events.dlx` de la cola de notificación de pólizas (renombrar implica recrear la cola).
6. Caos, carga y reconciliación (criterios de la fase 7). Pasado el periodo de seguridad, borrar el volumen `andina_clean_mongo_data`.

Para probar localmente: `--profile jsonpe-mock` con `JSONPE_BASE_URL=http://jsonpe-mock:8080` simula JSON.pe, y `--profile whatsapp-mock` con `WHATSAPP_BASE_URL=http://whatsapp-mock:8080` simula WhatsApp.
